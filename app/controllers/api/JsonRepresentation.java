package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static play.mvc.Results.status;

public class JsonRepresentation extends JacksonRepresentation {

    public static final String CONTENT_TYPE = "application/json; charset=utf-8";

    public JsonRepresentation() {
        super(makeObjectMapper(), CONTENT_TYPE);
    }

    private static ObjectMapper makeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return objectMapper;
    }

    public Result createdResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 202);
        result.put("message", "Record saved successfully");
        result.put("errors", emptyList());

        return status(202, asString(result)).as(CONTENT_TYPE);
    }

    public static JsonRepresentation instance = new JsonRepresentation();
}
