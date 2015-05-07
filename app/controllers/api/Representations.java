package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.conf.Register;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.validation.ValError;

import java.util.*;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;
import static uk.gov.openregister.config.ApplicationConf.registerName;

public class Representations {

    enum Representation {
        HTML,
        JSON
    }


    public static Result toResponse(Http.RequestHeader requestHeader, int status, String message) {
        Representation representation = representationFor(requestHeader.queryString());
        switch (representation) {
            case JSON:
                return toJsonResponse(status, message);
            case HTML:
                return toHtmlResponse(status, message);
            default:
                return toJsonResponse(400, "Unsupported representation '" + representation + "'");
        }
    }

    public static Result toListOfRecords(Http.Request request, List<Record> records) throws JsonProcessingException {

        Representation representation = representationFor(request.queryString());
        switch (representation) {
            case JSON:
                return ok(new ObjectMapper().writeValueAsString(records));
            case HTML:
                return ok(views.html.entries.render(Register.instance.registerInfo().keys, records));
            default:
                return toJsonResponse(400, "Unsupported representation '" + representation + "'");
        }
    }

    public static Result toRecord(Http.Request request, Optional<Record> recordO) {
        Representation representation = representationFor(request.queryString());
        switch (representation) {
            case JSON:
                return recordO.map(record -> ok(record.toString())).orElse(toJsonResponse(404, "Entry not found"));
            case HTML:
                return recordO.map(record -> ok(views.html.entry.render(Register.instance.registerInfo().keys, record)))
                        .orElse(toHtmlResponse(404, "Entry not found"));
            default:
                return toJsonResponse(400, "Unsupported representation '" + representation + "'");
        }
    }

    public static Results.Status toHtmlResponse(int status, String message) {
        return status(status, views.html.error.render(message));
    }


    public static Results.Status toJsonResponse(int statusCode, String message) {
        return toJsonResponse(statusCode, message, Collections.<ValError>emptyList());
    }

    public static Results.Status toJsonResponse(int statusCode, String message, List<ValError> errors) {
        Map<String,Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("message", message);
        result.put("errors", errors);

        return status(statusCode, (JsonNode) new ObjectMapper().valueToTree(result));
    }

    private static Representation representationFor(Map<String, String[]> queryString) {

        String[] representations = queryString.getOrDefault("_representation", new String[]{"html"});
        String representation = representations[0];

        if ("json".equalsIgnoreCase(representation)) {
            return Representation.JSON;
        } else {
            return Representation.HTML;
        }
    }

}

