package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.conf.Register;
import play.data.DynamicForm;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValError;
import uk.gov.openregister.validation.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application extends Controller {

    private static DynamicForm dynamicForm = new DynamicForm();


    public static Result index() {
        long count = Register.instance.store().count();
        return ok(views.html.index.render(Register.instance.registerInfo().keys, count));
    }

    public static Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(Register.instance.registerInfo().keys, dynamicForm));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result create() {

        DynamicForm dynamicForm = Application.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValError> validationErrors = new Validator(Register.instance.registerInfo().keys).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                Register.instance.store().save(record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {
                dynamicForm.reject(e.getMessage());
                return ok(views.html.newEntry.render(Register.instance.registerInfo().keys, dynamicForm));
            }
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));

        return ok(views.html.newEntry.render(Register.instance.registerInfo().keys, dynamicForm));
    }

    @SuppressWarnings("unchecked")
    public static Result renderUpdateEntryForm(String hash) {
        Record record = Register.instance.store().findByHash(hash).get();

        return ok(views.html.updateEntry.render(
                        Register.instance.registerInfo().keys,
                        Application.dynamicForm.bind(new ObjectMapper().convertValue(record.getEntry(), Map.class)),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result update(String hash) {
        DynamicForm dynamicForm = Application.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValError> validationErrors = new Validator(Register.instance.registerInfo().keys).validate(record);
        if (validationErrors.isEmpty()) {

            try {
                Register.instance.store().update(hash, record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {
                dynamicForm.reject(e.getMessage());
                return ok(views.html.updateEntry.render(
                        Register.instance.registerInfo().keys,
                        dynamicForm,
                        hash));
            }
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));
        return ok(views.html.updateEntry.render(
                Register.instance.registerInfo().keys,
                dynamicForm,
                hash));
    }

    private static Record createRecordFromParams(Map<String, String> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.keySet().stream()
                    .filter(Register.instance.registerInfo().keys::contains)
                    .forEach(key -> jsonMap.put(key, formParameters.get(key)));

            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

