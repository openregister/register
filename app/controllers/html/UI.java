package controllers.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import controllers.api.HtmlRepresentation;
import org.joda.time.DateTime;
import play.mvc.BodyParser;
import play.mvc.Result;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.DatabaseConflictException;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UI extends BaseController {

    public Result index() {
        long count = store.count();
        Optional<DateTime> lastUpdated = store.search("", 0, 1, Optional.empty())
                .stream().findFirst().map(Record::getLastUpdated);
        String lastUpdatedUI = lastUpdated
                .map(datetime -> datetime.toString("dd MMM yyyy"))
                .orElse("");

        return ok(views.html.index.render(register, count, lastUpdatedUI));
    }

    public Result renderNewEntryForm() {
        return ok(views.html.newEntry.render(register, Collections.emptyMap(), Collections.emptyMap()));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result create() {
        Map<String, String[]> requestParams = request.body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(register.name()), register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                store.save(record);
                return redirect("/hash/" + record.getHash());
            } catch (DatabaseException e) {
                return ok(views.html.newEntry.render(register, convertToMapOfListValues(requestParams), Collections.singletonMap("globalError", e.getMessage())));
            }
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));

        return ok(views.html.newEntry.render(register, convertToMapOfListValues(requestParams), errors));
    }

    @SuppressWarnings("unchecked")
    public Result renderUpdateEntryForm(String hash) {
        Record record = store.findByHash(hash).get();

        Map params = JsonObjectMapper.convert(record.getEntry(), Map.class);

        return ok(views.html.updateEntry.render(register,
                        convertToMapOfListValues(params),
                        Collections.emptyMap(),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result update(String hash) {
        Map<String, String[]> requestParams = request.body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(register.name()), register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                register.store().update(hash, record);
                return redirect("/hash/" + record.getHash());
            } catch (DatabaseConflictException e) {
                return new HtmlRepresentation(register).toResponse(409, e.getMessage());
            }
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));
        return ok(views.html.updateEntry.render(register,
                convertToMapOfListValues(requestParams),
                errors,
                hash));
    }

    private Map<String, List<String>> convertToMapOfListValues(Map<String, ?> requestParams) {
        Map<String, List<String>> stringListHashMap = new HashMap<>();
        for (String key : requestParams.keySet()) {
            if (requestParams.get(key) instanceof String[]) {
                stringListHashMap.put(key, Arrays.asList((String[]) requestParams.get(key)));
            } else if (requestParams.get(key) instanceof List) {
                stringListHashMap.put(key, (List) requestParams.get(key));
            } else {
                stringListHashMap.put(key, Arrays.asList((String) requestParams.get(key)));
            }
        }
        return stringListHashMap;
    }

    private Record createRecordFromQueryParams(Map<String, String[]> formParameters) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            formParameters.keySet().stream().filter(register.fieldNames()::contains).forEach(key -> {
                if (register.name().equals("register") && key.equals("fields")) {
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

