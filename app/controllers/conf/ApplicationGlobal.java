package controllers.conf;

import controllers.api.Representation;
import play.Application;
import play.GlobalSettings;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

import static controllers.api.Representations.representationFor;

public class ApplicationGlobal extends GlobalSettings {
    public static final String REPRESENTATION_QUERY_PARAM = "_representation";

    @Override
    public F.Promise<Result> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        Representation representation = representationFor(requestHeader.getQueryString(REPRESENTATION_QUERY_PARAM));
        return F.Promise.pure(
                representation.toResponse(500, throwable.getMessage())
        );
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader requestHeader) {
        Representation representation = representationFor(requestHeader.getQueryString(REPRESENTATION_QUERY_PARAM));
        return F.Promise.pure(
                representation.toResponse(404, "Page not found")
        );
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader requestHeader, String s) {
        Representation representation = representationFor(requestHeader.getQueryString(REPRESENTATION_QUERY_PARAM));
        return F.Promise.pure(
                representation.toResponse(400, s)
        );
    }

    @Override
    public void onStart(Application application) {
        Register.instance.init();
    }
}
