package controllers.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.App;
import controllers.api.HtmlRepresentation;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.model.Field;
import uk.gov.openregister.store.DatabaseConflictException;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

public class UI extends Controller {

    private final List<String> fieldNames;
    private final List<Field> fields;
    private final Store store;
    private final String registerName;

    public UI() {
        this.store = App.instance.register.store();
        this.fieldNames = App.instance.register.fieldNames();
        this.fields = App.instance.register.fields();
        this.registerName = App.instance.register.name();
    }

    public Result index() {
        long count = store.count();
        return ok(views.html.index.render(fieldNames, count));
    }

    public Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(registerName, fields, Collections.emptyMap(), Collections.emptyMap()));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result create() {

        Map<String, String[]> requestParams = request().body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(registerName), fieldNames).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                store.save(record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {

                return ok(views.html.newEntry.render(registerName, fields, convertToMapOfListValues(requestParams), Collections.singletonMap("globalError", e.getMessage())));
            }
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));

        return ok(views.html.newEntry.render(registerName, fields, convertToMapOfListValues(requestParams), errors));
    }

    @SuppressWarnings("unchecked")
    public Result renderUpdateEntryForm(String hash) {
        Record record = store.findByHash(hash).get().getRecord(

        );

        Map params = JsonObjectMapper.convert(record.getEntry().toString(), Map.class);

        return ok(views.html.updateEntry.render(registerName,
                        fields,
                        convertToMapOfListValues(params),
                        Collections.emptyMap(),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result update(String hash) {
        Map<String, String[]> requestParams = request().body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(registerName), fieldNames).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                store.update(hash, record);
            } catch (DatabaseConflictException e) {
                return HtmlRepresentation.instance.toResponse(409, e.getMessage());
            }
            return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));
        return ok(views.html.updateEntry.render(registerName,
                fields,
                convertToMapOfListValues(requestParams),
                errors,
                hash));
    }

    private Map<String, List<String>> convertToMapOfListValues(Map<String, ?> requestParams) {
        Map<String, List<String>> stringListHashMap = new HashMap<>();
        for (String key : requestParams.keySet()) {
            if (requestParams.get(key) instanceof String[]) {
                stringListHashMap.put(key, Arrays.asList((String[]) requestParams.get(key)));
            }else if (requestParams.get(key) instanceof List) {
                stringListHashMap.put(key, (List)requestParams.get(key));
            } else {
                stringListHashMap.put(key, Arrays.asList((String) requestParams.get(key)));
            }
        }
        return stringListHashMap;
    }

    private Record createRecordFromQueryParams(Map<String, String[]> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            formParameters.keySet().stream().filter(fieldNames::contains).forEach(key -> {
                if (registerName.equals("register") && key.equals("fields")) {
                    jsonMap.put(key, formParameters.get(key));
                } else {
                    jsonMap.put(key, formParameters.get(key)[0]);
                }
            });

            return new Record(new ObjectMapper().writeValueAsString(jsonMap));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TODO: json parsing exception, we need to address this when TODO above is done");
        }
    }
}

