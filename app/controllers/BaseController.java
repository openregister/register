package controllers;

import controllers.global.App;
import play.mvc.Controller;
import uk.gov.openregister.config.Register;

public class BaseController extends Controller {
    protected Register register() {
        return App.getRegister(App.registerName(request().host()));
    }
}



