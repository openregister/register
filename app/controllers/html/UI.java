package controllers.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.App;
import uk.gov.openregister.config.Register;
import play.data.DynamicForm;
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
import java.util.Map;

public class UI extends Controller {

    private static DynamicForm dynamicForm = new DynamicForm();


    public static Result index() {
        long count = App.instance.register.store().count();
        return ok(views.html.index.render(App.instance.register.fieldNames(), count));
    }

    public static Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(App.instance.register.fieldNames(), dynamicForm));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result create() {

        DynamicForm dynamicForm = UI.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(App.instance.register.name()), App.instance.register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                App.instance.register.store().save(record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {
                dynamicForm.reject(e.getMessage());
                return ok(views.html.newEntry.render(App.instance.register.fieldNames(), dynamicForm));
            }
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));

        return ok(views.html.newEntry.render(App.instance.register.fieldNames(), dynamicForm));
    }

    @SuppressWarnings("unchecked")
    public static Result renderUpdateEntryForm(String hash) {
        Record record = App.instance.register.store().findByHash(hash).get();

        return ok(views.html.updateEntry.render(
                        App.instance.register.fieldNames(),
                        UI.dynamicForm.bind(new ObjectMapper().convertValue(record.getEntry(), Map.class)),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result update(String hash) {
        DynamicForm dynamicForm = UI.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(App.instance.register.name()), App.instance.register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {

            try {
                App.instance.register.store().update(hash, record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {
                dynamicForm.reject(e.getMessage());
                return ok(views.html.updateEntry.render(
                        App.instance.register.fieldNames(),
                        dynamicForm,
                        hash));
            }
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));
        return ok(views.html.updateEntry.render(
                App.instance.register.fieldNames(),
                dynamicForm,
                hash));
    }

    private static Record createRecordFromParams(Map<String, String> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.keySet().stream()
                    .filter(App.instance.register.fieldNames()::contains)
                    .forEach(key -> jsonMap.put(key, formParameters.get(key)));

            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

