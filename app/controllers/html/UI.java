package controllers.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import controllers.api.HtmlRepresentation;
import org.joda.time.DateTime;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Result;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.store.DatabaseConflictException;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

public class UI extends BaseController {

    public Result index() {
        Register register =register();
        long count = register.store().count();
        String lastUpdatedUI = "";
        try {
            CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
            String rrUrl = curieResolver.resolve(new Curie("register", register.name())) + "?_representation=json";

            final WSResponse wsResponse = WS.client().url(rrUrl).execute().get(30000L);
            final String lastUpdatedStr = wsResponse.asJson().get("lastUpdated").textValue();
            final DateTime lastUpdated = DateTime.parse(lastUpdatedStr);
            lastUpdatedUI = lastUpdated.toString("dd MMM yyyy");
        } catch (Exception e) {
            //ignore
        }

        return ok(views.html.index.render(register, count, lastUpdatedUI));
    }

    public Result renderNewEntryForm() {
        Register register =register();
        return ok(views.html.newEntry.render(register, register.fields(), Collections.emptyMap(), Collections.emptyMap()));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result create() {
        Register register =register();
        Map<String, String[]> requestParams = request().body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(register.name()), register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                register.store().save(record);
                return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
            } catch (DatabaseException e) {

                return ok(views.html.newEntry.render(register, register.fields(), convertToMapOfListValues(requestParams), Collections.singletonMap("globalError", e.getMessage())));
            }
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));

        return ok(views.html.newEntry.render(register, register.fields(), convertToMapOfListValues(requestParams), errors));
    }

    @SuppressWarnings("unchecked")
    public Result renderUpdateEntryForm(String hash) {
        Register register =register();
        Record record = register.store().findByHash(hash).get();

        Map params = JsonObjectMapper.convert(record.getEntry(), Map.class);

        return ok(views.html.updateEntry.render(register,
                        register.fields(),
                        convertToMapOfListValues(params),
                        Collections.emptyMap(),
                        hash)
        );
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result update(String hash) {
        Register register =register();
        Map<String, String[]> requestParams = request().body().asFormUrlEncoded();

        Record record = createRecordFromQueryParams(requestParams);

        List<ValidationError> validationErrors = new Validator(Collections.singletonList(register.name()), register.fieldNames()).validate(record);
        if (validationErrors.isEmpty()) {
            try {
                register.store().update(hash, record);
            } catch (DatabaseConflictException e) {
                return HtmlRepresentation.instance.toResponse(409, e.getMessage(), register.friendlyName());
            }
            return redirect(controllers.api.routes.Rest.findByHash(record.getHash()));
        }
        Map<String, String> errors = validationErrors.stream().collect(Collectors.toMap(error -> error.key, error -> error.message));
        return ok(views.html.updateEntry.render(register,
                register.fields(),
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
        Register register =register();

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

