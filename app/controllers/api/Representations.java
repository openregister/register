package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.conf.Register;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.History;
import uk.gov.openregister.validation.ValidationError;

import java.util.*;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class Representations {

    enum Representation {
        HTML {
            @Override
            public Result toResponse(int status, String message) {
                return toHtmlResponse(status, message);
            }

            @Override
            public Result toListOfRecords(List<Record> records) throws JsonProcessingException {
                return ok(views.html.entries.render(Register.instance.fields(), records));
            }

            @Override
            public Result toRecord(Optional<Record> recordO, List<History> history) {
                return recordO.map(record ->
                        ok(views.html.entry.render(Register.instance.fields(), record, history)))
                        .orElse(toHtmlResponse(404, "Entry not found"));
            }
        },
        JSON {
            @Override
            public Result toResponse(int status, String message) {
                return toJsonResponse(status, message);
            }

            @Override
            public Result toListOfRecords(List<Record> records) throws JsonProcessingException {
                return ok(new ObjectMapper().writeValueAsString(records));
            }

            @Override
            public Result toRecord(Optional<Record> recordO, List<History> history) {
                return recordO.map(record -> ok(record.toString())).orElse(toJsonResponse(404, "Entry not found"));
            }
        };

        abstract public Result toResponse(int status, String message);
        abstract public Result toListOfRecords(List<Record> records) throws JsonProcessingException;
        abstract public Result toRecord(Optional<Record> recordO, List<History> history);
    }


    public static Result toResponse(Http.RequestHeader requestHeader, int status, String message) {
        Representation representation = representationFor(requestHeader.queryString());
        return representation.toResponse(status, message);
    }

    public static Result toListOfRecords(Http.Request request, List<Record> records) throws JsonProcessingException {
        Representation representation = representationFor(request.queryString());
        return representation.toListOfRecords(records);
    }

    public static Result toRecord(Http.Request request, Optional<Record> recordO, List<History> history) {
        Representation representation = representationFor(request.queryString());
        return representation.toRecord(recordO, history);
    }

    public static Results.Status toHtmlResponse(int status, String message) {
        return status(status, views.html.error.render(message));
    }


    public static Results.Status toJsonResponse(int statusCode, String message) {
        return toJsonResponse(statusCode, message, Collections.<ValidationError>emptyList());
    }

    public static Results.Status toJsonResponse(int statusCode, String message, List<ValidationError> errors) {
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

