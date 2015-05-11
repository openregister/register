package uk.gov.openregister.store.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.postgresql.util.PGobject;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.domain.Metadata;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.Store;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PostgresqlStore extends Store {

    private final RegisterInfo registerInfo;
    private Database database;

    public PostgresqlStore(String databaseURI, RegisterInfo registerInfo) {
        super(databaseURI);
        this.registerInfo = registerInfo;
        database = new Database(databaseURI);

        createTable(registerInfo.tableName);
    }

    public void createTable(String tableName) {
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb, metadata jsonb)");
    }

    @Override
    public void save(Record record) {

        String query = "INSERT INTO " + registerInfo.tableName + " (hash, entry, metadata) (" +
                "select ?,?,? " +
                "where not exists ( select 1 from " + registerInfo.tableName + " where entry @> ?)" +
                ")";

        int resultUpdated = database.executeUpdate(
                query,
                record.getHash(),
                createPGObject(record.getEntry().toString()),
                createPGObject(new Metadata(DateTime.now(), "").normalise()),
                createPGObject(JsonObjectMapper.convertToString(mapOf(registerInfo.primaryKey, primaryKeyValue(record))))
        );
        if (resultUpdated == 0) {
            throw new DatabaseException("No record inserted, a record with primary key value already exists");
        }
    }

    @Override
    public void update(String hash, Record record) {
        synchronized (hash.intern()) {

            String query = "INSERT INTO " + registerInfo.tableName + "(hash, entry, metadata) ( " +
                    "select ?,?,? " +
                    "where not exists  ( select 1 from " + registerInfo.tableName + " where metadata @> ? ) " +
                    "and exists ( select 1 from " + registerInfo.tableName + " where hash=? and entry @> ?)" +
                    ")";


            int resultUpdated = database.executeUpdate(
                    query,
                    record.getHash(),
                    createPGObject(record.getEntry().toString()),
                    createPGObject(new Metadata(DateTime.now(), hash).normalise()),
                    createPGObject(JsonObjectMapper.convertToString(mapOf("previousEntryHash", hash))),
                    hash,
                    createPGObject(JsonObjectMapper.convertToString(mapOf(registerInfo.primaryKey, primaryKeyValue(record))))
            );

            if (resultUpdated == 0) {
                throw new DatabaseException("No record updated");
            }
        }
    }

    @Override
    public void deleteAll() {
        database.execute("DROP TABLE IF EXISTS " + registerInfo.tableName);
        createTable(registerInfo.tableName);
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        return database.<Optional<Record>>select("SELECT * FROM " + registerInfo.tableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }'")
                .andThen(this::toOptionalRecord);
    }

    @Override
    public Optional<Record> findByHash(String hash) {

        return database.<Optional<Record>>select("SELECT * FROM " + registerInfo.tableName + " WHERE hash = ?", hash)
                .andThen(this::toOptionalRecord);

    }

    @Override
    public List<Record> search(Map<String, String> map) {
        String sql = "SELECT * FROM " + registerInfo.tableName;

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

        String sql = "SELECT * FROM " + registerInfo.tableName;

        if (!registerInfo.keys.isEmpty()) {
            List<String> where = registerInfo.keys.stream()
                    .map(k -> "entry->>'" + k + "' ILIKE '%" + query + "%'")
                    .collect(Collectors.toList());
            sql += " WHERE " + StringUtils.join(where, " OR ");
        }

        sql += " LIMIT 100";

        return database.<List<Record>>select(sql).andThen(this::getRecords);
    }

    @Override
    public long count() {
        return database.<Long>select("SELECT count(hash) FROM " + registerInfo.tableName + " AS count")
                .andThen(rs -> rs.next() ? rs.getLong("count") : 0);
    }

    //TODO: this will be deleted when we know the datatype of primary key
    private Object primaryKeyValue(Record record) {
        JsonNode jsonNode = record.getEntry().get(registerInfo.primaryKey);
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

    private Map<String, Object> mapOf(String key, Object value){
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

}

