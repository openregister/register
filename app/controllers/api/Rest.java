package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.BaseController;
import org.apache.commons.lang3.StringUtils;
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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

public class Rest extends BaseController {

    private static final String REPRESENTATION_QUERY_PARAM = "_representation";
    public static final int DEFAULT_RESULT_PAGE_SIZE = 30;
    private static final int ALL_ENTRIES_LIMIT = 1000;

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

    public F.Promise<Result> findByKey(String key, String value, String format) {
        Register register = register();
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> register.store().findByKV(key, URLDecoder.decode(value, "utf-8")));
        return recordF.map(record ->
                        getResponse(
                                record,
                                representationFrom(format),
                                //todo: . with format is required at this moment because the controller methods gets format with . in it
                                fmt -> routes.Rest.findByKey(key, value, "." + fmt).url(),
                                register.friendlyName()
                        )
        );
    }

    public F.Promise<Result> findByHash(String hash, String formatWithDot) {
        Register register = register();
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> register.store().findByHash(hash));
        return recordF.map(record ->
                        getResponse(
                                record,
                                representationFrom(formatWithDot),
                                //todo: . with format is required at this moment because the controller methods gets format with . in it
                                fmt -> routes.Rest.findByHash(hash, "." + fmt).url(),
                                register.friendlyName()
                        )
        );
    }

    public F.Promise<Result> all(String format, int page, int pageSize) throws Exception {
        Representation representation = representationFrom(format);

        return doSearch(page * pageSize, pageSize, representation)
                .map(records ->
                                representation.toListOfRecords(
                                        records,
                                        representationsMap(this::searchUriForFormat),
                                        page > 0 ? controllers.api.routes.Rest.all(format, page - 1, pageSize).absoluteURL(request()) : null,
                                        records.size() == pageSize ? controllers.api.routes.Rest.all(format, page + 1, pageSize).absoluteURL(request()) : null,
                                        register()
                                )
                );
    }

    public F.Promise<Result> search(String query, int page, int pageSize) throws Exception {
        Representation representation = representationFrom(null);

        return doSearch(page * pageSize, pageSize, representation)
                .map(records ->
                                representation.toListOfRecords(
                                        records,
                                        representationsMap(this::searchUriForFormat),
                                        page > 0 ? controllers.api.routes.Rest.all(query, page - 1, pageSize).absoluteURL(request()) : null,
                                        records.size() == pageSize ? controllers.api.routes.Rest.all(query, page + 1, pageSize).absoluteURL(request()) : null,
                                        register()
                                )
                );

    }

    private Result getResponse(Optional<Record> recordO, Representation representation, Function<String, String> routeForFormat, String registerName) {
        return recordO.map(record ->
                        representation.toRecord(record, getHistoryFor(record), representationsMap(routeForFormat), register())
        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found", registerName));
    }

    private F.Promise<List<Record>> doSearch(int offset, int limit, Representation representation) throws Exception {
        Register register = register();
        final int effectiveOffset;
        final int effectiveLimit;
        if (representation.isPaginated()) {
            effectiveOffset = offset;
            effectiveLimit = limit;
        } else {
            effectiveOffset = 0;
            effectiveLimit = ALL_ENTRIES_LIMIT;
        }

        return F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return register.store().search(request().queryString().get("_query")[0], effectiveOffset, effectiveLimit);
            } else {
                Map<String, String> map = request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .collect(toMap(Map.Entry::getKey, queryParamEntry -> queryParamEntry.getValue()[0]));
                return register.store().search(map, effectiveOffset, effectiveLimit);
            }
        });
    }

    private Representation representationFrom(String format) {
        if (StringUtils.isEmpty(format)) {
            String representationQueryValue = request().getQueryString(REPRESENTATION_QUERY_PARAM);
            if (representationQueryValue == null) {
                return Representations.Format.html.representation;
            } else {
                return Representations.representationFor(representationQueryValue);
            }
        } else {
            return Representations.representationFor(format.replaceAll("\\.(.*)", "$1"));
        }
    }

    private List<RecordVersionInfo> getHistoryFor(Record r) {
        return register().store().previousVersions(r.getHash());
    }

    private Map<String, String> representationsMap(Function<String, String> routeForFormat) {
        return Stream.of(Representations.Format.values())
                .map(Representations.Format::name)
                .collect(toMap(Function.<String>identity(), routeForFormat));
    }

    private String searchUriForFormat(String format) {
        // makes assumptions about the structure of the uri
        return request().uri() + "&" + REPRESENTATION_QUERY_PARAM + "=" + format;
    }
}
