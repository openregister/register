package controllers.api;

import controllers.html.Pagination;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class JsonRepresentation implements Representation {

    public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    public static final String JSONP_CONTENT_TYPE = "application/javascript; charset=utf-8";

    public Result createdResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 202);
        result.put("message", "Record saved successfully");
        result.put("errors", emptyList());

        return status(202, asString(emptyMap(), result)).as(JSON_CONTENT_TYPE);
    }

    public static JsonRepresentation instance = new JsonRepresentation();

    @Override
    public Result toListOfRecords(Register register,
                                  List<Record> records,
                                  Map<String, String[]> requestParams,
                                  Map<String, String> representationsMap,
                                  Pagination pagination) {
        Results.Status ok = ok(asString(requestParams, records));
        return requestParams.get("_callback") != null ? ok.as(JSONP_CONTENT_TYPE) : ok.as(JSON_CONTENT_TYPE);
    }

    @Override
    public Result toRecord(Register register,
                           Record record,
                           Map<String, String[]> requestParams,
                           Map<String, String> representationsMap,
                           List<RecordVersionInfo> history) {
        Results.Status ok = ok(asString(requestParams, record));
        return requestParams.get("_callback") != null ? ok.as(JSONP_CONTENT_TYPE) : ok.as(JSON_CONTENT_TYPE);
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    protected String asString(Map<String, String[]> requestParams, Object record) {
        String json = JsonObjectMapper.convertToString(record);

        String[] callbacks = requestParams.get("_callback");

        return callbacks != null ? callbacks[0] + "(" + json + ");" : json;
    }
}
