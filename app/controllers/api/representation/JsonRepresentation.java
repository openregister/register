package controllers.api.representation;

import controllers.html.Pagination;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.JsonObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class JsonRepresentation implements Representation {

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSONP_CONTENT_TYPE = "application/javascript";

    public Result createdResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("page", 202);
        result.put("total", "Record saved successfully");
        result.put("total_pages", emptyList());

        return status(202, asString(emptyMap(), result)).as(JSON_CONTENT_TYPE);
    }

    @Override
    public Result toListOfRecords(List<Record> records,
                                  Http.Request request,
                                  Pagination pagination) {

        //This is a mad hack to quickly put pagination
        //data into the results json
        Map<String, Object> meta = new HashMap<>();
        meta.put( "page", pagination.getPage() );
        meta.put( "page_size", pagination.getPageSize() );
        meta.put( "total_pages", pagination.getTotalPages());
        meta.put( "total_entries", pagination.getTotal());
        List<Object> data = new ArrayList<Object>();
        data.add(meta);
        data.addAll(records);

        String resultsString = asString(request.queryString(), data);

        Results.Status ok = ok(resultsString);
        return request.queryString().get("_callback") != null ? ok.as(JSONP_CONTENT_TYPE) : ok.as(JSON_CONTENT_TYPE);
    }

    @Override
    public Result toRecord(Record record,
                           Http.Request request,
                           List<RecordVersionInfo> history) {
        Results.Status ok = ok(asString(request.queryString(), record));
        return request.queryString().get("_callback") != null ? ok.as(JSONP_CONTENT_TYPE) : ok.as(JSON_CONTENT_TYPE);
    }

    @Override
    public boolean isPaginated() {
        return true;
    }

    protected String asString(Map<String, String[]> requestParams, Object record) {
        String json = JsonObjectMapper.convertToString(record);
        String[] callbacks = requestParams.get("_callback");
        return callbacks != null ? callbacks[0] + "(" + json + ");" : json;
    }
}
