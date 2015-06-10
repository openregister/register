package uk.gov.openregister.store.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.postgresql.util.PGobject;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseConflictException;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.SearchSpec;
import uk.gov.openregister.store.SearchSpec.SearchHelper;
import uk.gov.openregister.store.Store;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostgresqlStore implements Store {
    private final DBInfo dbInfo;
    private Database database;

    public final SearchHelper searchHelperUpdateTime = new SearchHelper() {
        // TODO sort also by entry ->> '%s' DESC, will impact performances
        private final String sqlTemplate = "  ORDER BY lastUpdated DESC";

        public String sortBy() {
            return sqlTemplate.replace("%s", dbInfo.primaryKey);
        }

        public boolean isHistoric() {
            return true;
        }
    };
    private SearchSpec searchSpec = new SearchSpec() {
        @Override
        public SearchHelper getDefault() {
            return searchHelperUpdateTime;
        }

        @Override
        public SearchHelper getLastUpdate() {
            return searchHelperUpdateTime;
        }
    };


    public PostgresqlStore(DBInfo dbInfo, DataSource dataSource) {
        this.dbInfo = dbInfo;
        this.database = new Database(dataSource);

        createTables(dbInfo.tableName);
    }

    @Override
    public SearchSpec getSearchSpec() {
        return searchSpec;
    }

    public void createTables(String tableName) {
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb, lastUpdated timestamp without time zone, previousEntryHash varchar(40), searchable tsvector)");
        database.execute("CREATE TABLE IF NOT EXISTS " + tableName + "_history (hash varchar(40) primary key,entry jsonb, lastUpdated timestamp without time zone, previousEntryHash varchar(40), searchable tsvector)");
        if (database.select("SELECT to_regclass('public." + tableName + "_lastUpdated_idx')")
                .andThen(r -> {
                    r.next();
                    return r.getString(1);
                }) == null) {
            database.execute("CREATE INDEX " + tableName + "_searchable_idx ON " + tableName + " USING gin(searchable)");
            database.execute("CREATE INDEX " + tableName + "_lastUpdated_idx ON " + tableName + " (lastUpdated DESC)");
            database.execute("CLUSTER " + tableName + " using " + tableName + "_lastUpdated_idx");
            database.execute("CREATE INDEX " + tableName + "_history_searchable_idx ON " + tableName + "_history USING gin(searchable)");
            database.execute("CREATE INDEX " + tableName + "_history_lastUpdated_idx ON " + tableName + "_history (lastUpdated DESC)");
            database.execute("CLUSTER " + tableName + "_history using " + tableName + "_history_lastUpdated_idx");
        }
    }

    @Override
    public void save(Record record) {

        String hash = record.getHash();
        PGobject entryObject = createPGObject(record.normalisedEntry());

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);

            final String searchableText = canonicalizeEntryText(record.normalisedEntry());
            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.tableName + " (hash, entry, lastUpdated, searchable) " +
                    "( select ?,?,?,to_tsvector(?)  where not exists ( select 1 from " + dbInfo.tableName + " where entry ->>?=?))")) {
                st.setObject(1, hash);
                st.setObject(2, entryObject);
                st.setTimestamp(3, new Timestamp(record.getLastUpdated().getMillis()));
                st.setString(4, searchableText);
                st.setObject(5, dbInfo.primaryKey);
                st.setObject(6, primaryKeyValue(record));
                int result = st.executeUpdate();
                if (result == 0) {
                    throw new DatabaseException("No record inserted, a record with primary key value already exists");
                }
            }

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.historyTableName + "(hash, entry, lastUpdated, searchable) VALUES(?, ?, ?, to_tsvector(?))")) {
                st.setObject(1, hash);
                st.setObject(2, entryObject);
                st.setTimestamp(3, new Timestamp(record.getLastUpdated().getMillis()));
                st.setString(4, searchableText);
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
            PGobject entryObject = createPGObject(record.normalisedEntry());
            entryObject.setType("jsonb");
            Timestamp timestamp = new Timestamp(record.getLastUpdated().getMillis());
            String searchableText = canonicalizeEntryText(record.normalisedEntry());

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.tableName + " (hash, entry, lastUpdated, previousEntryHash, searchable) " +
                    "( select ?,?,?,?,to_tsvector(?)  where exists ( select 1 from " + dbInfo.tableName + " where hash=? and entry ->>?=?))")) {
                st.setObject(1, newHash);
                st.setObject(2, entryObject);
                st.setTimestamp(3, timestamp);
                st.setObject(4, oldhash);
                st.setString(5, searchableText);
                st.setObject(6, oldhash);
                st.setObject(7, dbInfo.primaryKey);
                st.setObject(8, primaryKeyValue(record));

                int result = st.executeUpdate();
                if (result == 0) {
                    throw new DatabaseConflictException("Either this record is outdated or attempted to update the primary key value.");
                }
            }

            try (PreparedStatement st = connection.prepareStatement("DELETE FROM " + dbInfo.tableName + " where hash=?")) {
                st.setObject(1, oldhash);
                st.executeUpdate();
            }

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.historyTableName + "(hash, entry, lastUpdated, previousEntryHash, searchable) VALUES(?, ?, ?, ?, to_tsvector(?))")) {
                st.setObject(1, newHash);
                st.setObject(2, entryObject);
                st.setTimestamp(3, timestamp);
                st.setObject(4, oldhash);
                st.setString(5, searchableText);
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
        database.execute("DROP TABLE IF EXISTS " + dbInfo.historyTableName);
        createTables(dbInfo.tableName);
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        return database.<Optional<Record>>select("SELECT * FROM " + dbInfo.tableName + " WHERE entry ->>'" + key + "'='" + value + "'")
                .andThen(this::toOptionalRecord);
    }

    @Override
    public List<RecordVersionInfo> previousVersions(String hash) {
        // XXX hackity hack
        List<RecordVersionInfo> versions = new ArrayList<>();
        String currentHash = hash;
        while (true) {
            String previousHash = database.<String>select("SELECT previousEntryHash FROM " + dbInfo.historyTableName +
                            " WHERE hash = ?", currentHash
            ).andThen(resultSet -> {
                resultSet.next();
                return resultSet.getString("previousEntryHash");
            });
            if (previousHash == null || previousHash.isEmpty()) {
                break;
            }
            Record record = findByHash(previousHash).get();
            versions.add(
                    new RecordVersionInfo(record.getHash(), record.getLastUpdated())
            );
            currentHash = previousHash;
        }
        return versions;
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        return database.<Optional<Record>>select("SELECT * FROM " + dbInfo.historyTableName + " WHERE hash = ?", hash).andThen(this::toOptionalRecord);
    }

    @Override
    public List<Record> search(Map<String, String> map, int offset, int limit, Optional<SearchHelper> sortBy, boolean exact) {
        String sql = "";
        if (!map.isEmpty()) {
            List<String> where = map.keySet().stream()
                    .map(k -> toMatchStatement(map, k, exact))
                    .collect(Collectors.toList());
            sql += " WHERE " + StringUtils.join(where, " AND ");
        }

        return executeSearch(sql, offset, limit, sortBy);

    }

    private String toMatchStatement(Map<String, String> map, String k, boolean exact) {
        if (exact)
            return "entry->>'" + k + "'='" + map.get(k) + "'";
        else
            return "entry->>'" + k + "' ILIKE '%" + map.get(k) + "%'";
    }

    @Override
    public List<Record> search(String query, int offset, int limit, Optional<SearchHelper> sortBy) {
        String sql = "";
        if (!dbInfo.keys.isEmpty()) {
            String where = fullTrim(query);
            if(!query.trim().isEmpty())
                sql += " WHERE searchable @@ to_tsquery('" + where + "')";
        }

        return executeSearch(sql, offset, limit, sortBy);
    }

    private List<Record> executeSearch(String where, int offset, int limit, Optional<SearchHelper> sortBy) {
        String sql;
        if (sortBy.isPresent() && sortBy.get().isHistoric()) {
            sql = createQuery(where, offset, limit, sortBy, dbInfo.historyTableName);
        } else {
            sql = createQuery(where, offset, limit, sortBy, dbInfo.tableName);
        }

        return database.<List<Record>>select(sql).andThen(this::getRecords);
    }

    private String createQuery(String where, int offset, int limit, Optional<SearchHelper> sortBy, String tableName) {
        String sql = "SELECT * FROM " + tableName;

        sql += where;

        if (sortBy.isPresent()) {
            sql += sortBy.get().sortBy();
//        } else {
//            sql += searchSpec.getDefault().sortBy();
        }
        sql += " LIMIT " + limit;
        sql += " OFFSET " + offset;

        return sql;
    }

    @Override
    public long count() {
        return database.<Long>select("select to_char(reltuples, '9999999999')  from pg_class where oid = 'public." + dbInfo.tableName + "'::regclass")
                .andThen(rs -> rs.next() ? rs.getLong(1) : 0);
    }

    @Override
    public void fastImport(List<Record> records) {


        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);


            try (PreparedStatement st1 = connection.prepareStatement("INSERT INTO " + dbInfo.tableName + " (hash, entry, lastUpdated, searchable) VALUES(?, ?, ?, to_tsvector(?))");
                 PreparedStatement st2 = connection.prepareStatement("INSERT INTO " + dbInfo.historyTableName + "(hash, entry, lastUpdated, searchable) VALUES(?, ?, ?, to_tsvector(?))")) {

                for (Record record : records) {
                    String hash = record.getHash();
                    String searchableText = canonicalizeEntryText(record.normalisedEntry());
                    PGobject entryObject = createPGObject(record.normalisedEntry());
                    entryObject.setType("jsonb");
                    Timestamp timestamp = new Timestamp(record.getLastUpdated().getMillis());
                    st1.setObject(1, hash);
                    st1.setObject(2, entryObject);
                    st1.setTimestamp(3, timestamp);
                    st1.setString(4, searchableText);
                    st1.addBatch();

                    st2.setObject(1, hash);
                    st2.setObject(2, entryObject);
                    st2.setObject(3, timestamp);
                    st2.setString(4, searchableText);
                    st2.addBatch();
                }

                st1.executeBatch();
                st2.executeBatch();
            }
            connection.commit();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String canonicalizeEntryText(String entryText) {
        return entryText.replaceAll("[\\{\\}-]", " ")
                .replaceAll(",?\"[^\"]+\":", " ")
                .replaceAll("[\"']", " ")
                .replaceAll("[ \\t]{2,}", " ")
                .toLowerCase();
    }

    private String fullTrim(String toTrim) {
        return toTrim.replaceAll("[\\p{Blank}\\p{Punct}]+", " & ")
                    .toLowerCase();
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
        final String entry = resultSet.getString("entry");
        final Timestamp lastUpdated = resultSet.getTimestamp("lastUpdated");

        return new Record(new ObjectMapper().readValue(entry, JsonNode.class), new DateTime(lastUpdated.getTime()));
    }

    private List<Record> getRecords(ResultSet resultSet) throws SQLException, IOException {

        List<Record> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(toRecord(resultSet));
        }
        return result;
    }

}

