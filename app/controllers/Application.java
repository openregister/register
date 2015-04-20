package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.MongodbStore;
import uk.gov.openregister.store.PostgresqlStore;
import uk.gov.openregister.store.Store;

import java.net.URL;
import java.util.*;

import static controllers.JsonUtil.toJsonResponse;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render(ApplicationConf.getString("register.name")));
    }

    private static Store store;
    static {

        String uri = ApplicationConf.getString("store.uri");
        String name = ApplicationConf.getString("register.name");

        if (uri.startsWith("mongodb")) store = new MongodbStore(uri, name);
        else if (uri.startsWith("postgres"))store = new PostgresqlStore(uri, name);
        else throw new RuntimeException("Unable to find store for store.uri=" + uri);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {

        store.save(new Record(request().body().asJson()));
        return status(202);
    }

    public static Result findByKey(String key, String value) {
        return store.findByKV(key, value)
                .map(registerRow -> ok(registerRow.toString()))
                .orElse(toJsonResponse(404, "Entry not found"));
    }

    public static Result findByHash(String hash) {
        return store.findByHash(hash)
                .map(registerRow -> ok(registerRow.toString()))
                .orElse(toJsonResponse(404, "Entry not found"));
    }

    public static Result search() {

        Set<Map.Entry<String, String[]>> entries = request().queryString().entrySet();

        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<String, String[]> queryParameter : entries) {
            map.put(queryParameter.getKey(), queryParameter.getValue()[0]);
        }

        List<Record> search = store.search(map);
        try {
            return ok(new ObjectMapper().writeValueAsString(search));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static F.Promise<Result> load() {
        Optional<String[]> urlOpt = Optional.ofNullable(request().queryString().get("url"));

        if (urlOpt.isPresent()) {
            return F.Promise.promise(() -> readAndSaveToDb(new URL(urlOpt.get()[0])))
                    .map(i -> toJsonResponse(200, i + " records loaded successfully"));

        } else {
            return F.Promise.promise(() -> toJsonResponse(400, "'url' parameter is not defined"));
        }

    }


    // TODO bulk import?
    public static int readAndSaveToDb(URL url) throws Exception {

        char cs = url.toString().endsWith(".tsv") ? '\t' : ',';

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(cs).withHeader();
        MappingIterator<JsonNode> it = mapper.reader(JsonNode.class)
                .with(schema)
                .readValues(url);
        int counter = 0;
        while (it.hasNext()) {
            JsonNode rowAsNode = it.next();
            store.save(new Record(rowAsNode));
            counter++;
        }
        return counter;

    }

}
