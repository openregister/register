package uk.gov.openregister.store.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.postgresql.util.PGobject;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.domain.History;
import uk.gov.openregister.domain.Metadata;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.Store;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PostgresqlStore implements Store {

    private final DBInfo dbInfo;
    private Database database;

    public PostgresqlStore(DBInfo dbInfo, DataSource dataSource) {
        this.dbInfo = dbInfo;
        this.database = new Database(dataSource);

        createTables(dbInfo.tableName);
    }

    public void createTables(String tableName) {
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb, metadata jsonb)");
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + "_history (hash varchar(40) primary key,entry jsonb, metadata jsonb)");
    }

    @Override
    public void save(Record record) {

        String hash = record.getHash();
        PGobject entryObject = createPGObject(record.getEntry().toString());
        PGobject metadataObject = createPGObject(new Metadata(DateTime.now(), "").normalise());

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.tableName + " (hash, entry, metadata) " +
                    "( select ?,?,?  where not exists ( select 1 from " + dbInfo.tableName + " where entry @> ?))")) {
                st.setObject(1, hash);
                st.setObject(2, entryObject);
                st.setObject(3, metadataObject);
                st.setObject(4, createPGObject(JsonObjectMapper.convertToString(mapOf(dbInfo.primaryKey, primaryKeyValue(record)))));
                int result = st.executeUpdate();
                if (result == 0) {
                    throw new DatabaseException("No record inserted, a record with primary key value already exists");
                }
            }

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.historyTableName + "(hash, entry,metadata) VALUES(?, ?, ?)")) {
                st.setObject(1, hash);
                st.setObject(2, entryObject);
                st.setObject(3, metadataObject);
                st.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(String oldhash, Record record) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);

            String newHash = record.getHash();
            PGobject entryObject = createPGObject(record.getEntry().toString());
            PGobject metadataObject = createPGObject(new Metadata(DateTime.now(), oldhash).normalise());

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.tableName + " (hash, entry, metadata) " +
                    "( select ?,?,?  where exists ( select 1 from " + dbInfo.tableName + " where hash=? and entry @> ?))")) {
                st.setObject(1, newHash);
                st.setObject(2, entryObject);
                st.setObject(3, metadataObject);
                st.setObject(4, oldhash);
                st.setObject(5, createPGObject(JsonObjectMapper.convertToString(mapOf(dbInfo.primaryKey, primaryKeyValue(record)))));

                int result = st.executeUpdate();
                if (result == 0) {
                    throw new DatabaseException("No record updated");
                }
            }

            try (PreparedStatement st = connection.prepareStatement("DELETE FROM " + dbInfo.tableName + " where hash=?")) {
                st.setObject(1, oldhash);
                st.executeUpdate();
            }

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.historyTableName + "(hash, entry,metadata) VALUES(?, ?, ?)")) {
                st.setObject(1, newHash);
                st.setObject(2, entryObject);
                st.setObject(3, metadataObject);
                st.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll() {
        database.execute("DROP TABLE IF EXISTS " + dbInfo.tableName);
        createTables(dbInfo.tableName);
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        return database.<Optional<Record>>select("SELECT * FROM " + dbInfo.tableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }'")
                .andThen(this::toOptionalRecord);
    }

    @Override
    public List<History> history(String key, String value) {
        return database.<List<History>>select("SELECT hash,metadata::json->>'creationTime' as creationTime FROM " + dbInfo.historyTableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }' limit 100")
                .andThen((resultSet) -> {
                    List<History> allHistory = new ArrayList<>();
                    while (resultSet.next()) {
                        allHistory.add(new History(resultSet.getString("hash"), DateTime.parse(resultSet.getString("creationTime"))));
                    }
                    Collections.sort(allHistory);
                    return allHistory;
                });

    }

    @Override
    public Optional<Record> findByHash(String hash) {
        return database.<Optional<Record>>select("SELECT * FROM " + dbInfo.historyTableName + " WHERE hash = ?", hash).andThen(this::toOptionalRecord);
    }

    @Override
    public List<Record> search(Map<String, String> map) {
        String sql = "SELECT * FROM " + dbInfo.tableName;

        if (!map.isEmpty()) {
            List<String> where = map.keySet().stream()
                    .map(k -> "entry->>'" + k + "' ILIKE '%" + map.get(k) + "%'")
                    .collect(Collectors.toList());
            sql += " WHERE " + StringUtils.join(where, " AND ");
        }

        sql += " LIMIT 100";

        return database.<List<Record>>select(sql).andThen(this::getRecords);

    }

    @Override
    public List<Record> search(String query) {

        String sql = "SELECT * FROM " + dbInfo.tableName;

        if (!dbInfo.keys.isEmpty()) {
            List<String> where = dbInfo.keys.stream()
                    .map(k -> "entry->>'" + k + "' ILIKE '%" + query + "%'")
                    .collect(Collectors.toList());
            sql += " WHERE " + StringUtils.join(where, " OR ");
        }

        sql += " LIMIT 100";

        return database.<List<Record>>select(sql).andThen(this::getRecords);
    }

    @Override
    public long count() {
        return database.<Long>select("SELECT count(hash) FROM " + dbInfo.tableName + " AS count")
                .andThen(rs -> rs.next() ? rs.getLong("count") : 0);
    }

    //TODO: this will be deleted when we know the datatype of primary key
    private Object primaryKeyValue(Record record) {
        JsonNode jsonNode = record.getEntry().get(dbInfo.primaryKey);
        if (jsonNode instanceof TextNode) {
            return jsonNode.textValue();
        } else if (jsonNode instanceof IntNode) {
            return jsonNode.intValue();
        } else {
            throw new RuntimeException("Confirm is it acceptable???");
        }

    }

    private PGobject createPGObject(String data) {
        PGobject pgo = new PGobject();
        pgo.setType("jsonb");
        try {
            pgo.setValue(data);
        } catch (Exception e) { //success: api setter throws checked exception
        }
        return pgo;
    }

    private Optional<Record> toOptionalRecord(ResultSet resultSet) throws IOException, SQLException {
        if (resultSet != null && resultSet.next()) {
            return Optional.of(toRecord(resultSet));
        } else {
            return Optional.empty();
        }
    }

    private Record toRecord(ResultSet resultSet) throws SQLException, IOException {
        String entry = resultSet.getString("entry");
        return new Record(new ObjectMapper().readValue(entry, JsonNode.class));
    }

    private List<Record> getRecords(ResultSet resultSet) throws SQLException, IOException {

        List<Record> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(toRecord(resultSet));
        }
        return result;
    }

    private Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}

