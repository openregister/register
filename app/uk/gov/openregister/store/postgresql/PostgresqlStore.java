package uk.gov.openregister.store.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.Store;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostgresqlStore extends Store {

    private String tableName;
    private final List<String> keys;
    private Database database;

    public PostgresqlStore(String databaseURI, String tableName, List<String> keys) {
        super(databaseURI);
        this.tableName = tableName;
        this.keys = keys;
        database = new Database(databaseURI);

        createTable(tableName);
    }

    public void createTable(String tableName) {
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb)");
    }

    @Override
    public void save(Record record) {

        PGobject pgo = new PGobject();
        pgo.setType("jsonb");
        try { pgo.setValue(record.getEntry().toString()); } catch (Exception e) {}
        database.execute("INSERT INTO " + tableName + " (hash, entry) VALUES (?,?)", record.getHash(), pgo);

    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        return database.<Optional<Record>>select("SELECT * FROM " + tableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }'")
                .andThen(this::toOptionalRecord);
    }

    @Override
    public Optional<Record> findByHash(String hash) {

        return database.<Optional<Record>>select("SELECT * FROM " + tableName + " WHERE hash = ?", hash)
                .andThen(this::toOptionalRecord);

    }

    @Override
    public List<Record> search(Map<String, String> map) {
        String sql = "SELECT * FROM " + tableName;

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

        String sql = "SELECT * FROM " + tableName;

        if (!keys.isEmpty()) {
            List<String> where = keys.stream()
                    .map(k -> "entry->>'" + k + "' ILIKE '%" + query + "%'")
                    .collect(Collectors.toList());
            sql += " WHERE " + StringUtils.join(where, " OR ");
        }

        sql += " LIMIT 100";

        return database.<List<Record>>select(sql).andThen(this::getRecords);
    }

    @Override
    public long count() {
        return database.<Long>select("SELECT count(hash) FROM " + tableName + " AS count")
                .andThen(rs -> rs.next() ? rs.getLong("count") : 0);
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
}




