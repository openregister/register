package controllers.global;

import controllers.api.HtmlRepresentation;
import org.apache.commons.lang3.StringUtils;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

public class ApplicationGlobal extends GlobalSettings {
    @Override
    public F.Promise<Result> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        Logger.error("", throwable);

        //Can have specific responses based on error type
        return F.Promise.pure(
                HtmlRepresentation.instance.toResponse(500, throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage(), registerName(requestHeader))
        );
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader requestHeader) {
        return F.Promise.pure(
                HtmlRepresentation.instance.toResponse(404, "Page not found", registerName(requestHeader))
        );
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader requestHeader, String s) {
        return F.Promise.pure(
                HtmlRepresentation.instance.toResponse(400, s, registerName(requestHeader))
        );
    }

    private String registerName(Http.RequestHeader requestHeader) {
        return StringUtils.capitalize(App.registerName(requestHeader.host()));
    }

}
