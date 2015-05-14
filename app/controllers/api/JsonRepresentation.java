package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.validation.ValidationError;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Result toResponse(int status, String message) {
        return toResponseWithErrors(status, message, Collections.<ValidationError>emptyList());
    }

    public Results.Status toResponseWithErrors(int statusCode, String message, List<ValidationError> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("message", message);
        result.put("errors", errors);

        return status(statusCode, asString(result)).as(CONTENT_TYPE);
    }

    public static JsonRepresentation instance = new JsonRepresentation();
}
