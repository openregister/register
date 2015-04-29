package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import controllers.conf.Register;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static controllers.Representations.toJsonResponse;

public class Application extends Controller {

    public static Result index() {
        long count = Register.instance.store().count();
        return ok(views.html.index.render(ApplicationConf.getString("register.name"), count));
    }


    public static Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(ApplicationConf.getString("register.name"), Register.instance.keys()));
    }

    public static Result create() {
        String header = request().getHeader("Content-Type");
        if (header.contains(Http.MimeTypes.JSON)) {
            return createEntryFromJson();
        } else if (header.contains(Http.MimeTypes.FORM)) {
            return createEntryFromHtml();
        } else {
            throw new RuntimeException("Unsupported content type: " + header);
        }
    }

    public static F.Promise<Result> findByKey(String key, String value) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> Register.instance.store().findByKV(key, value));
        return recordF.map(record -> Representations.toRecord(request(), record));
    }

    public static F.Promise<Result> findByHash(String hash) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> Register.instance.store().findByHash(hash));
        return recordF.map(record -> Representations.toRecord(request(), record));
    }

    public static F.Promise<Result> search() {

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return Register.instance.store().search(request().queryString().get("_query")[0]);
            } else {
                HashMap<String, String> map = new HashMap<>();
                request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .forEach(queryParameter -> map.put(queryParameter.getKey(), queryParameter.getValue()[0]));
                return Register.instance.store().search(map);
            }
        });

        return recordsF.map(records -> Representations.toListOfRecords(request(), records));
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

    private static Result createEntryFromHtml() {
        Record record = createRecordFromParams(request().body().asFormUrlEncoded());
        Register.instance.store().save(record);
        return redirect("/hash/" + record.getHash());
    }

    private static Result createEntryFromJson() {
        Register.instance.store().save(new Record(request().body().asJson()));
        return toJsonResponse(202, "Record saved successfully");
    }

    // TODO bulk import?
    private static int readAndSaveToDb(URL url) throws Exception {

        char cs = url.toString().endsWith(".tsv") ? '\t' : ',';

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(cs).withHeader();
        MappingIterator<JsonNode> it = mapper.reader(JsonNode.class)
                .with(schema)
                .readValues(url);
        int counter = 0;
        while (it.hasNext()) {
            JsonNode rowAsNode = it.next();
            Register.instance.store().save(new Record(rowAsNode));
            counter++;
        }
        return counter;
    }

    private static Record createRecordFromParams(Map<String, String[]> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.forEach((k, v) -> jsonMap.put(k, v[0]));
            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }

}
