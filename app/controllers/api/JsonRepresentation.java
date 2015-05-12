package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.validation.ValidationError;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class JsonRepresentation implements Representation {

    private final ObjectMapper objectMapper;
    private final String contentType = "application/json; charset=utf-8";

    public JsonRepresentation() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public Result toResponse(int status, String message) {
        return toResponseWithErrors(status, message, Collections.<ValidationError>emptyList());
    }

    @Override
    public Result toListOfRecords(List<Record> records) throws JsonProcessingException {
        return ok(asString(records)).as(contentType);
    }

    @Override
    public Result toRecord(Optional<Record> recordO, List<RecordVersionInfo> history) {
        return recordO.map(record -> ok(asString(record)).as(contentType)).orElse(toResponseWithErrors(404, "Entry not found", Collections.<ValidationError>emptyList()));
    }

    private String asString(Object record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Results.Status toResponseWithErrors(int statusCode, String message, List<ValidationError> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("message", message);
        result.put("errors", errors);

        return status(statusCode, asString(result)).as(contentType);
    }

    public static JsonRepresentation instance = new JsonRepresentation();
}
