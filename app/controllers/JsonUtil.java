package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.Results;

import static play.mvc.Results.status;

public class JsonUtil {

    public static Results.Status toJsonResponse(int statusCode, String message) {
        ObjectNode result = Json.newObject();
        result.put("status", statusCode);
        result.put("message", message);
        return status(statusCode, result);
    }
}
