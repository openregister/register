package uk.gov.openregister;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JsonObjectMapperTest {
    @Test
    public void convertToString_Map_convertsMapToCononicalJson() {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("key1", "value1");
        jsonMap.put("akey", "value2");

        String result = JsonObjectMapper.convertToString(jsonMap);

        assertEquals("{\"akey\":\"value2\",\"key1\":\"value1\"}", result);

    }

}