package controllers.global;

import controllers.App;
import play.Application;
import play.GlobalSettings;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.config.Register;

import java.lang.reflect.Method;

import static controllers.api.Representations.toResponse;

public class ApplicationGlobal extends GlobalSettings {
    @Override
    public F.Promise<Result> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        return F.Promise.pure(
                toResponse(requestHeader, 500, throwable.getMessage())
        );
    }

    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader requestHeader) {
        return F.Promise.pure(
                toResponse(requestHeader, 404, "Page not found")
        );
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader requestHeader, String s) {
        return F.Promise.pure(
                toResponse(requestHeader, 400, s)
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