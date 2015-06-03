package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import controllers.html.Pagination;
import play.mvc.Result;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;

import static play.mvc.Results.ok;

public class YamlRepresentation implements Representation {
    private static final String CONTENT_TYPE = "text/yaml; charset=utf-8";

    public static Representation instance = new YamlRepresentation();

    @Override
    public Result toListOfRecords(Register register,
                                  List<Record> records,
                                  Map<String, String[]> requestParams,
                                  Map<String, String> representationsMap,
                                  Pagination pagination) {
        return ok(asString(records)).as(CONTENT_TYPE);
    }

    @Override
    public Result toRecord(Register register,
                           Record record,
                           Map<String, String[]> requestParams,
                           Map<String, String> representationsMap,
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
