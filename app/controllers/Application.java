package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import controllers.conf.Register;
import uk.gov.openregister.validation.ValidationResult;
import uk.gov.openregister.validation.Validator;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;

import java.util.HashMap;
import java.util.Map;

import static controllers.api.Representations.toHtmlResponse;

public class Application extends Controller {

    public static Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(ApplicationConf.getString("register.name"), Register.instance.keys()));
    }

    public static Result index() {
        long count = Register.instance.store().count();
        return ok(views.html.index.render(ApplicationConf.getString("register.name"), count));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public static Result create() {
        Record record = createRecordFromParams(request().body().asFormUrlEncoded());

        // Validation
        ValidationResult validationResult = new Validator(Register.instance.keys()).validate(record);
        if (!validationResult.isValid()) {
            return toHtmlResponse(400, Joiner.on(". ").join(validationResult.getMessages()));
        }

        Register.instance.store().save(record);
        return redirect("/hash/" + record.getHash());
    }


    public static Result docs() {
        return ok(views.html.docsIndex.render(ApplicationConf.getString("register.name")));
    }

    public static Result docsApi(String api) {
        switch(api) {
            case "create":
                 return ok(views.html.docsCreate.render(ApplicationConf.getString("register.name")));
            case "search":
                 return ok(views.html.docsSearch.render(ApplicationConf.getString("register.name")));
            case "get":
                 return ok(views.html.docsGet.render(ApplicationConf.getString("register.name")));
            default:
                return redirect("/docs");
        }
    }

    private static Record createRecordFromParams(Map<String, String[]> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            //TODO: this will break when we have multiple values for a key, data parsing will be based on datatype
            formParameters.keySet().stream()
                    .filter(key -> Register.instance.keys().contains(key))
                    .forEach(key -> jsonMap.put(key, formParameters.get(key)[0]));

            String json = new ObjectMapper().writeValueAsString(jsonMap);

            return new Record(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

