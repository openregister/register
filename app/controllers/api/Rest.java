package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.BaseController;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static controllers.api.Representations.representationFor;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

public class Rest extends BaseController {

    public static final String REPRESENTATION_QUERY_PARAM = "_representation";
    public static final String LIMIT_QUERY_PARAM = "_limit";
    private static final int DEFAULT_RESULT_SIZE = 100;

    @BodyParser.Of(BodyParser.Json.class)
    public Result create() throws JsonProcessingException {
        Register register = register();

        Record r = new Record(request().body().asJson());

        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                register.store().save(r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage(), register.friendlyName());
            }

            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors, register.friendlyName());

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String hash) {
        Register register = register();
        Record r = new Record(request().body().asJson());
        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                register.store().update(hash, r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage(), register.friendlyName());
            }
            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors, register.friendlyName());
    }

    public F.Promise<Result> findByKey(String key, String value) {
        return findByKeyWithFormat(key, value, representationQueryString());
    }

    public F.Promise<Result> findByKeyWithFormat(String key, String value, String format) {
        Register register = register();
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> register.store().findByKV(key, URLDecoder.decode(value, "utf-8")));
        return recordF.map(record -> getResponse(record, format,
                anyFormat -> controllers.api.routes.Rest.findByKeyWithFormat(key, value, anyFormat).url(), register.friendlyName()));
    }

    public F.Promise<Result> findByHash(String hash) {
        return findByHashWithFormat(hash, representationQueryString());
    }

    public F.Promise<Result> findByHashWithFormat(String hash, String format) {
        Register register = register();
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> register.store().findByHash(hash));
        return recordF.map(record -> getResponse(record, format,
                anyFormat -> controllers.api.routes.Rest.findByHashWithFormat(hash, anyFormat).url(), register.friendlyName()));
    }

    private String representationQueryString() {
        return request().getQueryString(REPRESENTATION_QUERY_PARAM);
    }

    private int limitQueryValue() {
        try {
            return Integer.parseInt(request().getQueryString(LIMIT_QUERY_PARAM));
        } catch (NullPointerException | NumberFormatException e) {
            return DEFAULT_RESULT_SIZE;
        }
    }

    private Result getResponse(Optional<Record> recordO, String format, Function<String, String> routeForFormat, String registerName) {
        final Representation representation;
        try {
            representation = representationFor(format);
        } catch (IllegalArgumentException e) {
            return formatNotRecognisedResponse(format, registerName);
        }
        return recordO.map(record ->
                        representation.toRecord(record, getHistoryFor(record), representationsMap(routeForFormat), register())
        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found", registerName));
    }

    public F.Promise<Result> search() {
        Register register = register();
        Representation representation;
        try {
            representation = representationFor(representationQueryString());
        } catch (IllegalArgumentException e) {
            return F.Promise.pure(formatNotRecognisedResponse(representationQueryString(), register.friendlyName()));
        }

        int limit = limitQueryValue();

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return register.store().search(request().queryString().get("_query")[0], limit);
            } else {
                Map<String, String> map = request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .collect(toMap(Map.Entry::getKey, queryParamEntry -> queryParamEntry.getValue()[0]));
                return register.store().search(map, limit);
            }
        });

        return recordsF.map(rs -> representation.toListOfRecords(rs, representationsMap(this::searchUriForFormat), register));
    }

    private Result formatNotRecognisedResponse(String format, String registerName) {
        return HtmlRepresentation.instance.toResponse(400, "Format '" + format + "' not recognised", registerName);
    }

    private List<RecordVersionInfo> getHistoryFor(Record r) {
        return register().store().previousVersions(r.getHash());
    }

    private Map<String, String> representationsMap(Function<String, String> routeForFormat) {
        return Stream.of(Representations.Format.values())
                .map(Representations.Format::name)
                .collect(toMap(Function.<String>identity(),
                        routeForFormat));
    }

    private String searchUriForFormat(String format) {
        // makes assumptions about the structure of the uri
        return request().uri() + "&" + REPRESENTATION_QUERY_PARAM + "=" + format;
    }
}
