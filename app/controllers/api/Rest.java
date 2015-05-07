package controllers.api;

import com.google.common.base.Joiner;
import controllers.conf.Register;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.validation.ValidationResult;
import uk.gov.openregister.validation.Validator;

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




}
