package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.postgresql.util.PGobject;
import uk.gov.openregister.domain.Record;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

// THIS IS A PROOF OF CONCEPTS
// THe implementation is not production ready, the queries are not optimised and are sql-injection safe.
public class PostgresqlStore extends Store {

    private ComboPooledDataSource cpds;
    private String tableName;
    private List<String> keys = Collections.emptyList();

    @Override
    public List<String> keys() {
        updateKeys();
        return keys;
    }

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
        Connection c = null;
        PreparedStatement st = null;

        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(record.getEntry().toString());

            String sql = insertQuery(tableName);
            c = getConnection();
            st = c.prepareStatement(sql);
            st.setString(1, record.getHash());
            st.setObject(2, pgo);

            st.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
        }
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        Connection c = null;
        Statement st = null;
        try {
            c = getConnection();
            st = c.createStatement();
            st.execute("SELECT * FROM " + tableName + " WHERE entry @> '" + "{ \"" + key + "\" : \"" + value + "\" }'");
            return toRecord(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
        }
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        Connection c = null;
        PreparedStatement st = null;

        try {
            c = getConnection();
            st = c.prepareStatement("SELECT * FROM " + tableName + " WHERE hash = ?");
            st.setString(1, hash);
            st.execute();
            return toRecord(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
        }
    }

    @Override
    public List<Record> search(Map<String, String> map) {

        Connection c = null;
        Statement st = null;
        try {
            c = getConnection();
            st = c.createStatement();

            String sql = "SELECT * FROM " + tableName;

            if (!map.isEmpty()) {
                List<String> where = map.keySet().stream()
                        .map(k -> "entry->>'" + k + "' ILIKE '%" + map.get(k) + "%'")
                        .collect(Collectors.toList());
                sql += " WHERE " + StringUtils.join(where, " AND ");
            }

            sql += " LIMIT 100";

            st.execute(sql);
            return getRecords(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
        }

    }

    @Override
    public List<Record> search(String query) {

        updateKeys();

        Connection c = null;
        Statement st = null;
        try {
            c = getConnection();
            st = c.createStatement();

            String sql = "SELECT * FROM " + tableName;

            if (!keys.isEmpty()) {
                List<String> where = keys.stream()
                        .map(k -> "entry->>'" + k + "' ILIKE '%" + query + "%'")
                        .collect(Collectors.toList());
                sql += " WHERE " + StringUtils.join(where, " OR ");
            }

            sql += " LIMIT 100";

            st.execute(sql);
            return getRecords(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
        }
    }

    private void updateKeys() {
        if(keys.isEmpty()) {
            // TODO This is a hack, the list of keys should be provided. Registers register?

            Connection c = null;
            PreparedStatement st = null;

            try {
                c = getConnection();
                st = c.prepareStatement("SELECT * FROM " + tableName + " LIMIT 1");
                st.execute();
                Optional<Record> recordOptional = toRecord(st);
                if (recordOptional.isPresent()) {
                    keys = Lists.newArrayList(recordOptional.get().getEntry().fieldNames());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(c != null) try {c.close();} catch (Exception e) {}
                if(st != null) try {st.close();} catch (Exception e) {}
            }
        }
    }

    @Override
    public long count() {
        Connection c = null;
        PreparedStatement st = null;

        try {
            c = getConnection();
            st = c.prepareStatement("SELECT count(hash) FROM " + tableName + " AS count");
            st.execute();
            ResultSet rs = st.getResultSet();
            return rs.next() ? rs.getLong("count") : 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c != null) try {c.close();} catch (Exception e) {}
            if(st != null) try {st.close();} catch (Exception e) {}
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
        resultSet.close();
        return result;
    }


    private Connection getConnection() throws ClassNotFoundException, SQLException {
        return cpds.getConnection();
    }
}
