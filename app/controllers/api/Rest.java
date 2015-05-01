package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Joiner;
import controllers.conf.Register;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.validation.ValidationResult;
import uk.gov.openregister.validation.Validator;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static controllers.api.Representations.toJsonResponse;

public class Rest extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {
        Record r = new Record(request().body().asJson());
        // Validation
        ValidationResult validationResult = new Validator(Register.instance.keys()).validate(r);
        if (!validationResult.isValid()) {
            // TODO, incomplete, needs better error messages
            return toJsonResponse(400, "The following keys are not allowed in the record: " + Joiner.on(", ").join(validationResult.getInvalidKeys()));
        }
        Register.instance.store().save(r);
        return toJsonResponse(202, "Record saved successfully");
    }

    //TODO: do validation
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String hash) {
        Record r = new Record(request().body().asJson());
        ValidationResult validationResult = new Validator(Register.instance.keys()).validate(r);

        if (!validationResult.isValid()) {
            return toJsonResponse(400, "The following keys are not allowed in the record: " + Joiner.on(", ").join(validationResult.getInvalidKeys()));
        }

        Register.instance.store().update(hash, ApplicationConf.getString("register.primaryKey"), r);
        return toJsonResponse(202, "Record saved successfully");
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

}
