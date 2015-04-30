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

package helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresqlStoreForTesting {
    public static final String POSTGRESQL_URI = "postgresql://localhost/testopenregister";

    public static void createTable(String tableName) throws SQLException, ClassNotFoundException {
        Statement st = getStatement();
        st.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (hash varchar(40) primary key,entry jsonb)");
        st.close();
    }

    public static void dropTable(String tableName) throws Exception {
        Statement st = getStatement();
        st.execute("DROP TABLE IF EXISTS " + tableName);
        st.close();
    }

    public static List<JsonNode> findAll(String tableName) throws Exception {
        List<JsonNode> result = new ArrayList<>();
        Statement st = null;
        ResultSet rs = null;
        try {
            st = getStatement();
            st.execute("SELECT * FROM " + tableName);
            rs = st.getResultSet();
            while(rs.next()){
                result.add(new ObjectMapper().readValue(rs.getString("entry"), JsonNode.class));
            }
        } finally {
            if (rs != null) rs.close();
            if (st != null) st.close();
        }
        return result;

    }

    public static JsonNode findFirstEntry(String tableName) throws Exception {
        return findAll(tableName).get(0);
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
