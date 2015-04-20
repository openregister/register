package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import uk.gov.openregister.domain.Record;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// THIS IS A PROOF OF CONCEPTS
// THe implementation is not production ready, the queries are not optimised and are sql-injection safe.
public class PostgresqlStore extends Store {

    private ComboPooledDataSource cpds;
    private String tableName;

    public static String insertQuery(String tableName) {
        return "INSERT INTO " + tableName
                + " (hash, entry) VALUES"
                + "(?,?)";
    }

    public static String createTableQuery(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb)";
    }

    public PostgresqlStore(String databaseURI, String tableName) {
        super(databaseURI);
        this.tableName = tableName;

        createConnectionPool();
        createTable(tableName);
    }

    private ComboPooledDataSource createConnectionPool() {
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.postgresql.Driver");
            cpds.setJdbcUrl("jdbc:" + databaseURI);
            return cpds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createTable(String tableName) {
        try {
            Statement st = getConnection().createStatement();
            st.execute(createTableQuery(tableName));
            st.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(Record record) {

        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(record.getEntry().toString());

            String sql = insertQuery(tableName);
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, record.getHash());
            preparedStatement.setObject(2, pgo);

            preparedStatement.execute();
            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        try {
            Statement st = getConnection().createStatement();
            st.execute("SELECT * FROM " + tableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }'");
            return toRecord(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        try {
            PreparedStatement st = getConnection().prepareStatement("SELECT * FROM " + tableName + " WHERE hash = ?");
            st.setString(1, hash);
            st.execute();
            return toRecord(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Record> search(Map<String, String> map) {
        try {
            Statement st = getConnection().createStatement();

            String sql = "SELECT * FROM " + tableName;

            if (!map.isEmpty()) {
                List<String> where = map.keySet().stream()
                        .map(k -> "entry->>'" + k + "' ILIKE '%" + map.get(k) + "%'")
                        .collect(Collectors.toList());
                sql += " WHERE " + StringUtils.join(where, "AND");
            }

            sql += " LIMIT 100";

            st.execute(sql);
            return getRecords(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }

    private Optional<Record> toRecord(Statement st) throws SQLException, java.io.IOException {
        ResultSet resultSet = st.getResultSet();
        if (resultSet != null && resultSet.next()) {
            return Optional.of(toRecord(resultSet));
        } else {
            return Optional.empty();
        }
    }

    private Record toRecord(ResultSet resultSet) throws SQLException, java.io.IOException {
        String entry = resultSet.getString("entry");
        return new Record(new ObjectMapper().readValue(entry, JsonNode.class));
    }

    private List<Record> getRecords(Statement st) throws SQLException, java.io.IOException {

        List<Record> result = new ArrayList<>();
        ResultSet resultSet = st.getResultSet();
        while (resultSet.next()) {
            result.add(toRecord(resultSet));
        }

        return result;
    }


    private Connection getConnection() throws ClassNotFoundException, SQLException {
        return cpds.getConnection();
    }
}
