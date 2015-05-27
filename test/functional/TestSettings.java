package functional;

import helper.PostgresqlStoreForTesting;

import java.util.HashMap;
import java.util.Map;

public class TestSettings {

    public static Map<String, String> settings() {
        HashMap<String, String> map = new HashMap<>();
        map.put("db.default.url", PostgresqlStoreForTesting.POSTGRESQL_URI);
        map.put("registers.service.template.url", "http://localhost:8888");
        return map;
    }

}
