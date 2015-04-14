package controllers;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.MongodbStore;
import uk.gov.openregister.store.Store;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render(ApplicationConf.getString("register.name")));
    }

    private static final Store store = new MongodbStore(ApplicationConf.getString("store.uri"), ApplicationConf.getString("register.name"));

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {

        store.create(new Record(request().body().asJson()));
        return status(202);
    }

    public static Result findByKey(String key, String value) {
        return store.findByKV(key, value)
                .map(registerRow -> ok(registerRow.toString()))
                .orElse(notFound());
    }

    public static Result findByHash(String hash) {
        return store.findByHash(hash)
                .map(registerRow -> ok(registerRow.toString()))
                .orElse(notFound());
    }
}
