package controllers.global;

import org.apache.commons.lang3.StringUtils;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import static play.mvc.Results.status;

public class ApplicationGlobal extends GlobalSettings {
    @Override
    public F.Promise<Result> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        Logger.error("", throwable);

        //Can have specific responses based on error type
        return F.Promise.pure(
                status(500, views.html.error.render(registerName(requestHeader), throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage())));
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader requestHeader) {
        return F.Promise.pure(
                status(404, views.html.error.render(registerName(requestHeader), "Page not found"))
        );
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader requestHeader, String s) {
        return F.Promise.pure(
                status(400, views.html.error.render(registerName(requestHeader), s))
        );
    }

    // For CORS requests
    @Override
    public Action<?> onRequest(Http.Request request,
                               java.lang.reflect.Method actionMethod) {
        return new ActionWrapper(super.onRequest(request, actionMethod));
    }

    private class ActionWrapper extends Action.Simple {
        public ActionWrapper(Action<?> action) {
            this.delegate = action;
        }

        @Override
        public F.Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
            F.Promise<Result> result = this.delegate.call(ctx);
            Http.Response response = ctx.response();
            response.setHeader("Access-Control-Allow-Origin", "*");
            return result;
        }
    }

    private String registerName(Http.RequestHeader requestHeader) {
        return StringUtils.capitalize(App.registerName(requestHeader.host()));
    }

}
