package controllers;

import play.Play;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.store.MongodbStore;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.domain.Entry;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render(ApplicationConf.getString("register.name")));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {

        Store store = new MongodbStore(ApplicationConf.getString("store.uri"), ApplicationConf.getString("register.name"));
        store.create(new Entry(request().body().asJson()));
        return status(202);
    }
}
