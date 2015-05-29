package helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.dbcp2.BasicDataSource;
import uk.gov.openregister.domain.Metadata;

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

        try (Connection conn = DriverManager.getConnection(POSTGRESQL_URI); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + " (hash varchar(40) primary key,entry json,metadata json)");
            st.execute("CREATE TABLE IF NOT EXISTS " + normalized(tableName) + "_history (hash varchar(40) primary key,entry json,metadata json)");
        }
    }

    private static String normalized(String tableName) {
        return tableName.replaceAll("-", "_");
    }

    public static void dropTables(String tableName) throws Exception {

        try (Connection conn = DriverManager.getConnection(POSTGRESQL_URI); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName));
            st.execute("DROP TABLE IF EXISTS " + normalized(tableName) + "_history");
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
