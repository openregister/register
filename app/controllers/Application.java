package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.conf.Register;
import play.data.DynamicForm;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.validation.ValidationResult;
import uk.gov.openregister.validation.Validator;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.openregister.config.ApplicationConf.registerName;

public class Application extends Controller {

    private static DynamicForm dynamicForm = new DynamicForm();

    public static Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(registerName, Register.instance.keys(), dynamicForm));
    }

    @SuppressWarnings("unchecked")
    public static Result renderUpdateEntryForm(String hash) {
        Record record = Register.instance.store().findByHash(hash).get();

        return ok(views.html.updateEntry.render(registerName,
                        Register.instance.keys(),
                        Application.dynamicForm.bind(new ObjectMapper().convertValue(record.getEntry(), Map.class)),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result update(String hash) {
        DynamicForm dynamicForm = Application.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        ValidationResult validationResult = new Validator(Register.instance.keys()).validate(record);
        if (!validationResult.isValid()) {


            validationResult.getMissingKeys()
                    .forEach(k -> dynamicForm.reject(k, "error.required"));

            return ok(views.html.updateEntry.render(registerName,
                            Register.instance.keys(),
                            dynamicForm,
                            hash)
            );
        }

        Register.instance.store().update(hash, registerName.toLowerCase(), record);
        return redirect("/hash/" + record.getHash());
    }

    public static Result index() {
        long count = Register.instance.store().count();
        return ok(views.html.index.render(registerName, Register.instance.keys(), count));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result create() {

        DynamicForm dynamicForm = Application.dynamicForm.bindFromRequest(request());
        Record record = createRecordFromParams(dynamicForm.data());

        // Validation
        ValidationResult validationResult = new Validator(Register.instance.keys()).validate(record);
        if (!validationResult.isValid()) {


            validationResult.getMissingKeys()
                    .forEach(k -> dynamicForm.reject(k, "error.required"));

            return ok(views.html.newEntry.render(registerName, Register.instance.keys(), dynamicForm));
        }

        Register.instance.store().save(record);
        return redirect("/hash/" + record.getHash());
    }


    public static Result docs() {
        return ok(views.html.docsIndex.render(registerName));
    }

    public static Result docsApi(String api) {
        switch (api) {
            case "create":
                return ok(views.html.docsCreate.render(registerName));
            case "search":
                return ok(views.html.docsSearch.render(registerName));
            case "get":
                return ok(views.html.docsGet.render(registerName));
            default:
                return redirect("/docs");
        }
    }

    private static Record createRecordFromParams(Map<String, String> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.keySet().stream()
                    .filter(key -> Register.instance.keys().contains(key))
                    .forEach(key -> jsonMap.put(key, formParameters.get(key)));

            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

