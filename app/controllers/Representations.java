package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;

import java.util.List;
import java.util.Optional;

import static controllers.JsonUtil.toJsonResponse;
import static play.mvc.Results.ok;

public class Representations {


    enum Representation {
        HTML,
        JSON
    }

    public static Result toListOfRecords(Http.Request request, List<String> keys, List<Record> records) throws JsonProcessingException {

        Representation representation = representationFor(request);
        switch (representation) {
            case JSON:
                return ok(new ObjectMapper().writeValueAsString(records));
            case HTML:
                return ok(views.html.entries.render(ApplicationConf.getString("register.name"), keys, records));
            default:
                return toJsonResponse(400, "Unsupported representation '" + representation + "'");
        }

    }

    public static Result toRecord(Http.Request request, List<String> keys, Optional<Record> recordO) {
        return recordO.map(record -> ok(record.toString())).orElse(toJsonResponse(404, "Entry not found"));
    }


    private static Representation representationFor(Http.Request request) {

        String[] representations = request.queryString().getOrDefault("_representation", new String[]{"html"});
        String representation = representations[0];

        if ("json".equalsIgnoreCase(representation)) {
            return Representation.JSON;
        } else {
            return Representation.HTML;
        }
    }

}
