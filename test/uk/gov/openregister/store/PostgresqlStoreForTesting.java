package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.openregister.conf.TestConfigurations;

import java.sql.*;

public class PostgresqlStoreForTesting {


    public static void dropTable(String tableName) throws Exception {
        Statement st = getStatement();
        st.execute("DROP TABLE IF EXISTS " + tableName);
        st.close();
    }

    public static JsonNode findFirstEntry(String tableName) throws Exception {

        Statement st = null;
        ResultSet rs = null;
        try {
            st = getStatement();
            st.execute("SELECT * FROM " + tableName);
            rs = st.getResultSet();
            rs.next();
            String entry = rs.getString("entry");
            return new ObjectMapper().readValue(entry, JsonNode.class);
        } finally {
            if (rs != null) rs.close();
            if (st != null) st.close();
        }
    }

    private static Statement getStatement() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection("jdbc:" + TestConfigurations.POSTGRESQL_URI);
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
