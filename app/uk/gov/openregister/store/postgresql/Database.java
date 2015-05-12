package uk.gov.openregister.store.postgresql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {

    private DataSource connectionPool;

    public Database(DataSource dataSource) {
        connectionPool = dataSource;
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

    public void execute(String sql, Object... params) {
        select(sql, params).andThen(rs -> true);
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }
}
