package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import controllers.conf.Register;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.validation.ValError;
import uk.gov.openregister.validation.Validator;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static controllers.api.Representations.toJsonResponse;

public class Rest extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() throws JsonProcessingException {
        Record r = new Record(request().body().asJson());

        List<ValError> validationErrors = new Validator(Register.instance.keys()).validate(r);

        if (validationErrors.isEmpty()) {
            Register.instance.store().save(r);
            return toJsonResponse(202, "Record saved successfully");
        }

        return toJsonResponse(400, "", validationErrors);

    }

    //TODO: do validation
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String hash) {
        Record r = new Record(request().body().asJson());
        List<ValError> validationErrors = new Validator(Register.instance.keys()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                Register.instance.store().update(hash, ApplicationConf.registerName.toLowerCase(), r);
            } catch (Exception e) {
                return toJsonResponse(400, e.getMessage());
            }
            return toJsonResponse(202, "Record saved successfully");
        }

        return toJsonResponse(400, "", validationErrors);
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
