package helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.dbcp2.BasicDataSource;
import org.joda.time.DateTime;
import uk.gov.openregister.crypto.Digest;
import uk.gov.openregister.domain.Metadata;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostgresqlStoreForTesting {
    public static final String POSTGRESQL_URI = "jdbc:postgresql://localhost/testopenregister";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataSource dataSource() {
        try {
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(POSTGRESQL_URI);
            dataSource.setInitialSize(1);
            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTables(String tableName) throws SQLException, ClassNotFoundException {

        try (Connection conn = DriverManager.getConnection(POSTGRESQL_URI); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + " (hash varchar(40) primary key,entry json,metadata json)");
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + "_history (hash varchar(40) primary key,entry json,metadata json)");
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + "_record (hash varchar(40) primary key, entry json)");
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + "_version (hash varchar(40) primary key, records json, signature varchar(40), parent varchar(40), version_number bigserial, creation_time timestamp)");
            String hash;
            DateTime creationTime = DateTime.now();
            try {
                hash = fakedOutHashOfVersion(creationTime, new ObjectMapper().readValue("{}", ObjectNode.class), "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            st.execute("INSERT INTO " + normalized(tableName) + "_version (hash, records, creation_time) " +
                            " SELECT '" + hash + "', '{}', '" + new Timestamp(creationTime.getMillis()) + "'" +
                            " WHERE NOT EXISTS (SELECT 1 FROM " + normalized(tableName) + "_version)");
        }
    }

    private static String fakedOutHashOfVersion(DateTime creationTime, ObjectNode records, String parentHash) {
        // XXX duplicated from NewPostgresqlStore; needs to account for whole version entry
        return Digest.shasum(creationTime.toString() + records.toString() + parentHash);
    }

    private static String normalized(String tableName) {
        return tableName.replaceAll("-", "_");
    }

    public static void dropTables(String tableName) throws Exception {

        try (Connection conn = DriverManager.getConnection(POSTGRESQL_URI); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName));
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName) + "_history");
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName) + "_record");
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName) + "_version");
        }
    }

    public static List<DataRow> findAll(String tableName) {
        List<DataRow> result = new ArrayList<>();
        try {
            ResultSet rs = null;

            try (Connection conn = DriverManager.getConnection(POSTGRESQL_URI); Statement st = conn.createStatement()) {

                st.execute("SELECT * FROM " + normalized(tableName));
                rs = st.getResultSet();
                while (rs.next()) {
                    result.add(new DataRow(
                                    rs.getString("hash"),
                                    new ObjectMapper().readValue(rs.getString("entry"), JsonNode.class),
                                    Metadata.from(rs.getString("metadata"))
                            )
                    );
                }
            } finally {
                if (rs != null) rs.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("error", e);
        }

        return result;
    }
}
