package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import controllers.html.Pagination;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;

import static play.mvc.Results.ok;

public class YamlRepresentation implements Representation {
    private static final String CONTENT_TYPE = "text/yaml; charset=utf-8";

    @Override
    public Result toListOfRecords(List<Record> records,
                                  Http.Request request,
                                  Pagination pagination) {
        return ok(asString(records)).as(CONTENT_TYPE);
    }

    @Override
    public Result toRecord(Record record,
                           Http.Request request,
                           List<RecordVersionInfo> history) {
        return ok(asString(record)).as(CONTENT_TYPE);
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    protected String asString(Object record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper objectMapper = JsonObjectMapper.objectMapper(new YAMLFactory());
}
