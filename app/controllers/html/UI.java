package controllers.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.App;
import play.data.DynamicForm;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UI extends Controller {

    private final List<String> fieldNames;
    private final Store store;
    private final String registerName;

    public UI() {
        this.store = App.instance.register.store();
        this.fieldNames = App.instance.register.fieldNames();
        this.registerName = App.instance.register.name();
    }

    public Result index() {
        long count = store.count();
        return ok(views.html.index.render(fieldNames, count));
    }

    public Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(fieldNames, new DynamicForm()));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result create() {

        DynamicForm dynamicForm = new DynamicForm().bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(registerName), fieldNames).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                store.save(record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {
                dynamicForm.reject(e.getMessage());
                return ok(views.html.newEntry.render(fieldNames, dynamicForm));
            }
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));

        return ok(views.html.newEntry.render(fieldNames, dynamicForm));
    }

    @SuppressWarnings("unchecked")
    public Result renderUpdateEntryForm(String hash) {
        Record record = store.findByHash(hash).get();

        return ok(views.html.updateEntry.render(
                        fieldNames,
                        new DynamicForm().bind(new ObjectMapper().convertValue(record.getEntry(), Map.class)),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result update(String hash) {
        DynamicForm dynamicForm = new DynamicForm().bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(registerName), fieldNames).validate(record);
        if (validationErrors.isEmpty()) {
            store.update(hash, record);
            return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
        }
        validationErrors.stream().forEach(error -> dynamicForm.reject(error.key, "error.required"));
        return ok(views.html.updateEntry.render(
                fieldNames,
                dynamicForm,
                hash));
    }

    private  Record createRecordFromParams(Map<String, String> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.keySet().stream()
                    .filter(fieldNames::contains)
                    .forEach(key -> jsonMap.put(key, formParameters.get(key)));

            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

