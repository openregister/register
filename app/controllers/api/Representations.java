package controllers.api;

import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Optional;

public class Representations {

    public static final String REPRESENTATION = "_representation";

    public static Result toResponse(Http.RequestHeader requestHeader, int status, String message) {
        Representation representation = representationFor(requestHeader.getQueryString(REPRESENTATION));
        return representation.toResponse(status, message);
    }

    public static Result toListOfRecords(Http.Request request, List<Record> records) throws Exception {
        Representation representation = representationFor(request.getQueryString(REPRESENTATION));
        return representation.toListOfRecords(records);
    }

    public static Result toRecord(Http.Request request, Optional<Record> recordO, List<RecordVersionInfo> history) {
        Representation representation = representationFor(request.getQueryString(REPRESENTATION));
        return representation.toRecord(recordO, history);
    }

    private static Representation representationFor(String representation) {
        if ("json".equalsIgnoreCase(representation)) {
            return JsonRepresentation.instance;
        } else {
            return HtmlRepresentation.instance;
        }
    }

}

