/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public <T> FunctionThatThrows<FunctionThatThrows<ResultSet, T>, T> select(String sql, Object... params ) {
        return (FunctionThatThrows<ResultSet, T> p) -> {
            Connection c = null;
            PreparedStatement st = null;
            try {

                c = connectionPool.getConnection();
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
