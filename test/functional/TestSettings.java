package functional;

import helper.PostgresqlStoreForTesting;

import java.util.HashMap;
import java.util.Map;

public class TestSettings {

    public static Map<String, String> forRegister(String name) {
        HashMap<String, String> map = new HashMap<>();
        map.put("db.default.url", PostgresqlStoreForTesting.POSTGRESQL_URI);
        map.put("register.name", name);
        map.put("registers.service.url", "http://localhost:8888");
        return map;
    }

}
