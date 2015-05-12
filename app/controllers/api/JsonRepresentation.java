package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Override
    public Result toResponse(int status, String message) {
        return toResponseWithErrors(status, message, Collections.<ValidationError>emptyList());
    }

    @Override
    public Result toListOfRecords(List<Record> records) throws JsonProcessingException {
        return ok(new ObjectMapper().writeValueAsString(records));
    }

    @Override
    public Result toRecord(Optional<Record> recordO, List<RecordVersionInfo> history) {
        return recordO.map(record -> ok(record.toString())).orElse(toResponseWithErrors(404, "Entry not found", Collections.<ValidationError>emptyList()));
    }

    public static Results.Status toResponseWithErrors(int statusCode, String message, List<ValidationError> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("message", message);
        result.put("errors", errors);

        return status(statusCode, (JsonNode) new ObjectMapper().valueToTree(result));
    }

    public static Representation instance = new JsonRepresentation();
}
