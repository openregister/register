package functional;

import java.util.HashMap;
import java.util.Map;

public class TestSettings {

    public static Map<String, String> settings() {
        HashMap<String, String> map = new HashMap<>();
        map.put("db.default.url", "postgres://localhost:5432/testopenregister");
        map.put("registers.service.template.url", "http://localhost:8888");
        return map;
    }

}
