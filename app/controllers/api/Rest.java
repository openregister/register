package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.App;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static controllers.api.Representations.toJsonResponse;

public class Rest extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() throws JsonProcessingException {
        Record r = new Record(request().body().asJson());

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(App.instance.register.name()), App.instance.register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                App.instance.register.store().save(r);
            } catch (DatabaseException e) {
                return toJsonResponse(400, e.getMessage());
            }

            return toJsonResponse(202, "Record saved successfully");
        }

        return toJsonResponse(400, "", validationErrors);

    }

    //TODO: do validation
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String hash) {
        Record r = new Record(request().body().asJson());
        List<ValidationError> validationErrors = new Validator(Collections.singletonList(App.instance.register.name()), App.instance.register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                App.instance.register.store().update(hash, r);
            } catch (DatabaseException e) {
                return toJsonResponse(400, e.getMessage());
            }
            return toJsonResponse(202, "Record saved successfully");
        }

        return toJsonResponse(400, "", validationErrors);
    }

    public static F.Promise<Result> findByKey(String key, String value) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> App.instance.register.store().findByKV(key, value));
        return recordF.map(record -> Representations.toRecord(request(), record));
    }

    public static F.Promise<Result> findByHash(String hash) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> App.instance.register.store().findByHash(hash));
        return recordF.map(record -> Representations.toRecord(request(), record));
    }

    public static F.Promise<Result> search() {

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return App.instance.register.store().search(request().queryString().get("_query")[0]);
            } else {
                HashMap<String, String> map = new HashMap<>();
                request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .forEach(queryParameter -> map.put(queryParameter.getKey(), queryParameter.getValue()[0]));
                return App.instance.register.store().search(map);
            }
        });

        return recordsF.map(records -> Representations.toListOfRecords(request(), records));
    }



}
