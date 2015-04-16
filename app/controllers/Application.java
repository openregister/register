package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.MongodbStore;
import uk.gov.openregister.store.Store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static Result search() {

        Set<Map.Entry<String, String[]>> entries = request().queryString().entrySet();

        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<String,String[]> queryParameter : entries) {
            map.put(queryParameter.getKey(), queryParameter.getValue()[0]);
        }

        List<Record> search = store.search(map);
        try {
            return ok(new ObjectMapper().writeValueAsString(search));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
