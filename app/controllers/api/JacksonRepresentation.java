package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class JacksonRepresentation implements Representation {
    protected final ObjectMapper objectMapper;
    private final String contentType;

    public JacksonRepresentation(ObjectMapper objectMapper, String contentType) {
        this.objectMapper = objectMapper;
        this.contentType = contentType;
    }

    public Result toResponse(int status, String message) {
        return toResponseWithErrors(status, message, Collections.<ValidationError>emptyList());
    }

    @Override
    public Result toListOfRecords(List<Record> records) throws JsonProcessingException {
        return ok(asString(records)).as(contentType);
    }

    @Override
    public Result toRecord(Record record, List<RecordVersionInfo> history) {
        return ok(asString(record)).as(contentType);
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
}
