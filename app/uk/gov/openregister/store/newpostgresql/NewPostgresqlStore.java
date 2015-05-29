package uk.gov.openregister.store.newpostgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.postgresql.util.PGobject;
import uk.gov.openregister.crypto.Digest;
import uk.gov.openregister.domain.Metadata;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.SortType;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.Database;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class NewPostgresqlStore implements Store {

    private final DBInfo dbInfo;
    private Database database;

    public NewPostgresqlStore(DBInfo dbInfo, DataSource dataSource) {
        this.dbInfo = dbInfo;
        this.database = new Database(dataSource);

        createTables(dbInfo);
    }

    public void createTables(DBInfo dbInfo) {
        database.execute("CREATE TABLE IF NOT EXISTS " + dbInfo.recordTableName +
                " (hash varchar(40) primary key, entry json)");
        database.execute("CREATE TABLE IF NOT EXISTS " + dbInfo.versionTableName +
                " (hash varchar(40) primary key, records json, signature varchar(40), parent varchar(40), version_number bigserial, creation_time timestamp)");
        DateTime creationTime = DateTime.now();
        String hash;
        try {
            hash = fakedOutHashOfVersion(creationTime, new ObjectMapper().readValue("{}", ObjectNode.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        database.execute("INSERT INTO " + dbInfo.versionTableName + " (hash, records, creation_time) " +
                        "VALUES(?, '{}', ?)",
                hash,
                new Timestamp(creationTime.getMillis()));
    }

    private String fakedOutHashOfVersion(DateTime creationTime, ObjectNode records) {
        // XXX needs to account for whole version entry
        return Digest.shasum(creationTime.toString() + records.toString());
    }

    @Override
    public void save(Record record) {
        String recordHash = record.getHash();
        PGobject entryObject = createPGObject(record.normalisedEntry());

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);

            // pessimistic lock to prevent concurrent updates causing divergent history
            try (PreparedStatement st = connection.prepareStatement("LOCK TABLE " + dbInfo.versionTableName + " IN EXCLUSIVE MODE")) {
                st.execute();
            }

            // insert record
            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.recordTableName + " (hash, entry) " +
                    " SELECT ?,? " +
                    " WHERE NOT EXISTS (SELECT 1 FROM " + dbInfo.recordTableName + " WHERE hash=?)")) {
                st.setObject(1, recordHash);
                st.setObject(2, entryObject);
                st.setObject(3, recordHash);
                st.executeUpdate();
            }

            // insert version
            String parentHash;
            ObjectNode records;
            try (PreparedStatement st = connection.prepareStatement("SELECT hash,records FROM " + dbInfo.versionTableName + " ORDER BY creation_time DESC LIMIT 1")) {
                ResultSet resultSet = st.executeQuery();
                if (!resultSet.next()) {
                    throw new RuntimeException("Couldn't find latest version");
                }
                parentHash = resultSet.getString("hash");
                records = new ObjectMapper().readValue(resultSet.getString("records"), ObjectNode.class);
            }
            records.put(record.getEntry().get(dbInfo.primaryKey).asText(), recordHash);

            DateTime creationTime = DateTime.now();
            String versionHash = fakedOutHashOfVersion(creationTime, records);

            try (PreparedStatement st = connection.prepareStatement("INSERT INTO " + dbInfo.versionTableName + " (hash, records, parent, creation_time) VALUES(?, ?, ?, ?)")) {
                st.setObject(1, versionHash);
                st.setObject(2, createPGObject(records.toString()));
                st.setObject(3, parentHash);
                st.setObject(4, new Timestamp(creationTime.getMillis()));
                st.executeUpdate();
            }

            connection.commit();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll() {
        // not implemented
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        // only supports primary key lookups, ignores "key" param
        return database.<Optional<Record>>select(
                "SELECT creation_time, hash, entry " +
                        " FROM (SELECT creation_time, records->>'" + value + "' AS hash FROM " + dbInfo.versionTableName + " ORDER BY version_number DESC LIMIT 1) q1 " +
                        " LEFT JOIN LATERAL (SELECT entry FROM " + dbInfo.recordTableName + " WHERE hash = q1.hash) q2 " +
                        " ON TRUE")
                .andThen(this::toOptionalRecord);
    }

    @Override
    public List<RecordVersionInfo> previousVersions(String hash) {
        // not implemented
        return emptyList();
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        return database.<Optional<Record>>select(
                "SELECT creation_time, hash, entry " +
                        " FROM (SELECT hash, entry FROM " + dbInfo.recordTableName + " WHERE hash = ?) q1 " +
                        // FIXME better query for creation_time
                        // not trivial as the same hash could be present in multiple versions
                        " LEFT JOIN LATERAL (SELECT 'now'::timestamp AS creation_time) q2 " +
                        " ON TRUE", hash)
                .andThen(this::toOptionalRecord);
    }

    @Override
    public List<Record> search(Map<String, String> map, int offset, int limit, SortType.SortBy Key) {
        return null;
    }

    @Override
    public List<Record> search(String query, int offset, int limit, SortType.SortBy sortBy) {
        return null;
    }

    @Override
    public long count() {
        // not implemented
        return 0;
    }

    @Override
    public SortType getSortType() {
        return null;
    }

    @Override
    public void update(String hash, Record record) {
        // not implemented
    }

    private PGobject createPGObject(String data) {
        PGobject pgo = new PGobject();
        pgo.setType("json");
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
        String hash = resultSet.getString("hash");
        Timestamp creation_time = resultSet.getTimestamp("creation_time");

        Record record = new Record(new ObjectMapper().readValue(entry, JsonNode.class), Optional.of(new Metadata(new DateTime(creation_time), null)));

        // on-the-fly integrity checking :)
        if (!Objects.equals(record.getHash(), hash)) {
            throw new IllegalStateException("stored hash " + hash + " didn't match calculated hash " + record.getHash());
        }
        return record;
    }
}
