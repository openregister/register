package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Result;
import uk.gov.openregister.domain.DbRecord;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;

import static play.mvc.Results.ok;

public class JacksonRepresentation implements Representation {
    private final ObjectMapper objectMapper;
    private final String contentType;

    public JacksonRepresentation(ObjectMapper objectMapper, String contentType) {
        this.objectMapper = objectMapper;
        this.contentType = contentType;
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap) throws JsonProcessingException {
        return ok(asString(records)).as(contentType);
    }

    @Override
    public Result toRecord(DbRecord dbRecord, List<RecordVersionInfo> history, Map<String, String> representationsMap) {
        return ok(asString(dbRecord.getRecord())).as(contentType);
    }

    protected String asString(Object record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
