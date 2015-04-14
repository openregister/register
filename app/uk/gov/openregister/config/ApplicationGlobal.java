package uk.gov.openregister.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.GlobalSettings;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;

import static play.mvc.Results.*;

public class ApplicationGlobal extends GlobalSettings {
    @Override
    public F.Promise<Result> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        return F.Promise.<Result>pure(internalServerError(
                toJsonError(500, throwable.getMessage())
        ));
    }

    private JsonNode toJsonError(int status, String message) {
        ObjectNode result = Json.newObject();
        result.put("status", status);
        result.put("message", message);
        return result;
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader requestHeader) {
        return F.Promise.<Result>pure(notFound(
                toJsonError(404, "Action not found")
        ));
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader requestHeader, String s) {
        return F.Promise.<Result>pure(badRequest(
                toJsonError(400, s)
        ));
    }

}
