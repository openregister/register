package helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.openregister.domain.Metadata;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PostgresqlStoreForTesting {
    public static final String POSTGRESQL_URI = "postgresql://localhost/testopenregister";

    public static void createTable(String tableName) throws SQLException, ClassNotFoundException {
        try(Statement st = getStatement()){
            st.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb,metadata jsonb)");
        }
    }

    public static void dropTable(String tableName) throws Exception {
        try(Statement st = getStatement()) {
            st.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    public static List<JsonNode> findAllEntries(String tableName) {
        return findAll(tableName).stream().map(t -> t.entry).collect(Collectors.toList());
    }

    public static List<DataRow> findAll(String tableName) {
        List<DataRow> result = new ArrayList<>();
        try {
            ResultSet rs = null;
            try(Statement st = getStatement()) {

                st.execute("SELECT * FROM " + tableName);
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

    private static Statement getStatement() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection("jdbc:" + POSTGRESQL_URI);
        return conn.createStatement();
    }

    public static String findFirstHash(String tableName) throws SQLException, ClassNotFoundException {

        Statement st = getStatement();
        st.execute("SELECT * FROM " + tableName);
        ResultSet resultSet = st.getResultSet();
        resultSet.next();
        return resultSet.getString("hash");
    }
}
