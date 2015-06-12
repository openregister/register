package helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.dbcp2.BasicDataSource;
import org.joda.time.DateTime;
import uk.gov.openregister.store.postgresql.DBInfo;
import uk.gov.openregister.store.postgresql.Database;
import uk.gov.openregister.store.postgresql.DatabaseSchema;

import javax.sql.DataSource;
import java.sql.*;
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
        new DatabaseSchema(new Database(dataSource()), new DBInfo(normalized(tableName), normalized(tableName), null)).createIfNotExist();
    }

    public static void dropTables(String tableName) throws Exception {
        new DatabaseSchema(new Database(dataSource()), new DBInfo(normalized(tableName), normalized(tableName), null)).drop();
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
                                    new DateTime(rs.getTimestamp("lastUpdated").getTime()),
                                    rs.getString("previousEntryHash")
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

    private static String normalized(String tableName) {
        return tableName.replaceAll("-", "_");
    }
}
