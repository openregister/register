package controllers;

import controllers.global.App;
import play.mvc.Controller;
import play.mvc.Http;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.store.Store;

public class BaseController extends Controller {
    protected final Register register;
    protected final Store store;
    protected final Http.Request request;

    protected BaseController() {
        this.request = request();
        this.register = App.getRegister(App.registerName(request().host()));
        this.store = register.store();
    }
}



