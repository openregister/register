package controllers.global;

import controllers.App;
import controllers.api.Representation;
import play.Application;
import play.GlobalSettings;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.lang.reflect.Method;

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
        App.instance.init();
    }

    @Override
    public Action onRequest(Http.Request request, Method method) {
        if(!App.instance.started()) {
            return new Action.Simple(){

                @Override
                public F.Promise<Result> call(Http.Context context) throws Throwable {
                    return F.Promise.pure(status(500, views.html.bootstrapError.render(App.instance.register.friendlyName() + " Register bootstrap failed", App.instance.getInitErrors())));
                }
            };
        }
        return super.onRequest(request, method);
    }
}
