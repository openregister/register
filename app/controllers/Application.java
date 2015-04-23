package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.mongodb.MongodbStore;
import uk.gov.openregister.store.postgresql.PostgresqlStore;
import uk.gov.openregister.store.Store;

import java.net.URL;
import java.util.*;

import static controllers.Representations.toJsonResponse;

public class Application extends Controller {

    public static Result index() {
        long count = store.count();
        return ok(views.html.index.render(ApplicationConf.getString("register.name"), count));
    }

    private static Store store;
    private static List<String> keys;

    static {

        String uri = ApplicationConf.getString("store.uri");
        String name = ApplicationConf.getString("register.name");

        if (uri.startsWith("mongodb")) store = new MongodbStore(uri, name);
        else if (uri.startsWith("postgres")) store = new PostgresqlStore(uri, name);
        else throw new RuntimeException("Unable to find store for store.uri=" + uri);

        // TODO This is a temporary workaround. The list of keys for this register should come from the registers register
        keys = store.keys();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {

        store.save(new Record(request().body().asJson()));
        return toJsonResponse(202, "Record saved successfully");
    }

    public static F.Promise<Result> findByKey(String key, String value) {
        if(keys.isEmpty()) keys = store.keys(); // TODO remove this line once the keys are loaded from th registers register
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByKV(key, value));
        return recordF.map(record -> Representations.toRecord(request(), keys, record));
    }

    public static F.Promise<Result> findByHash(String hash) {
        if(keys.isEmpty()) keys = store.keys(); // TODO remove this line once the keys are loaded from th registers register
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByHash(hash));
        return recordF.map(record -> Representations.toRecord(request(), keys, record));
    }

    public static F.Promise<Result> search() {
        if(keys.isEmpty()) keys = store.keys(); // TODO remove this line once the keys are loaded from th registers register

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return store.search(request().queryString().get("_query")[0]);
            } else {
                HashMap<String, String> map = new HashMap<>();
                request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .forEach(queryParameter -> map.put(queryParameter.getKey(), queryParameter.getValue()[0]));
                return store.search(map);
            }
        });

        return recordsF.map(records -> Representations.toListOfRecords(request(), keys, records));
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
