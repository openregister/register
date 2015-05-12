package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonRepresentation extends JacksonRepresentation {
    public JsonRepresentation() {
        super(makeObjectMapper(), "application/json; charset=utf-8");
    }

    private static ObjectMapper makeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return objectMapper;
    }

    public static JacksonRepresentation instance = new JsonRepresentation();
}
