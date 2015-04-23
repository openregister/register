package uk.gov.openregister.store.postgresql;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Database {


    private ComboPooledDataSource cpds;

    public Database(String databaseURI) {
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.postgresql.Driver");
            cpds.setJdbcUrl("jdbc:" + databaseURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> FunctionThatThrows<FunctionThatThrows<ResultSet, T>, T> select(String sql, Object... params ) {
        return (FunctionThatThrows<ResultSet, T> p) -> {
            Connection c = null;
            PreparedStatement st = null;
            try {

                c = cpds.getConnection();
                st = c.prepareStatement(sql);
                for (int i = 0; i < params.length; i++) {
                    st.setObject(i + 1, params[i]);
                }
                st.execute();
                return p.andThen(st.getResultSet());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (c != null) try { c.close(); } catch (Exception e) { }
                if (st != null) try { st.close(); } catch (Exception e) { }
            }
        };
    }


    public void execute(String sql, Object... params ) {
        select(sql, params).andThen(rs -> true);
    }
}
