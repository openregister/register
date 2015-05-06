package uk.gov.openregister.store.postgresql;

import org.apache.commons.dbcp2.BasicDataSource;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Database {


    private BasicDataSource connectionPool;

    public Database(String databaseURI) {
        try {

            URI dbUri = new URI(databaseURI);
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();
            connectionPool = new BasicDataSource();

            if (dbUri.getUserInfo() != null) {
                connectionPool.setUsername(dbUri.getUserInfo().split(":")[0]);
                connectionPool.setPassword(dbUri.getUserInfo().split(":")[1]);
            }
            connectionPool.setDriverClassName("org.postgresql.Driver");
            connectionPool.setUrl(dbUrl);
            connectionPool.setInitialSize(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> FunctionThatThrows<FunctionThatThrows<ResultSet, T>, T> select(String sql, Object... params) {
        return (FunctionThatThrows<ResultSet, T> p) -> {
            try (Connection c = connectionPool.getConnection();
                 PreparedStatement st = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    st.setObject(i + 1, params[i]);
                }
                st.execute();
                return p.andThen(st.getResultSet());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public int executeUpdate(String sql, Object... params) {
        try (Connection c = connectionPool.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                st.setObject(i + 1, params[i]);
            }
            return st.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void execute(String sql, Object... params) {
        select(sql, params).andThen(rs -> true);
    }
}
