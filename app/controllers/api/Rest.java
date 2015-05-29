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
import uk.gov.openregister.store.SortType;
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
                                //todo: . with format is required at this moment because the controller methods receives format starts with '.'
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
                                //todo: . with format is required at this moment because the controller methods receives format starts with '.'
                                fmt -> routes.Rest.findByHash(hash, "." + fmt).url(),
                                register.friendlyName()
                        )
        );
    }

    public F.Promise<Result> all(String format, int page, int pageSize) throws Exception {
        return findByQuery(
                format,
                request().getQueryString("_query"),
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.all(format, p, ps).absoluteURL(request()),
                register().store().getSortType().getDefault());
    }

    public F.Promise<Result> latest(String format, int page, int pageSize) throws Exception {
        return findByQuery(
                format,
                request().getQueryString("_query"),
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.latest(format, p, ps).absoluteURL(request()),
                register().store().getSortType().getLastUpdate());
    }

    public F.Promise<Result> search(String query, int page, int pageSize) throws Exception {

        return findByQuery(
                request().getQueryString(REPRESENTATION_QUERY_PARAM),
                query,
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.search(q, p, ps).absoluteURL(request()),
                register().store().getSortType().getDefault());
    }

    public F.Promise<Result> findByQuery(String format, String query, int page, int pageSize, PaginationUrlFunction paginationUrlFunction, SortType.SortBy sortBy) throws Exception {
        Register register = register();
        Representation representation = representationFrom(format);

        List<Record> records = doSearch(page * pageSize, pageSize, representation, register, sortBy);

        return F.Promise.promise(() -> representation.toListOfRecords(
                records,
                representationsMap(this::searchUriForFormat),
                page > 0 ? paginationUrlFunction.apply(query, page - 1, pageSize) : null,
                records.size() == pageSize ? paginationUrlFunction.apply(query, page + 1, pageSize): null,
                register()
        ));
    }

    @FunctionalInterface
    private interface PaginationUrlFunction {
        String apply(String query, int page, int pageSize);
    }

    private Result getResponse(Optional<Record> recordO, Representation representation, Function<String, String> routeForFormat, String registerName) {
        return recordO.map(record ->
                        representation.toRecord(record, getHistoryFor(record), representationsMap(routeForFormat), register())
        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found", registerName));
    }

    private List<Record> doSearch(int offset, int limit, Representation representation, Register register, SortType.SortBy sortBy) throws Exception {

        final int effectiveOffset;
        final int effectiveLimit;
        if (representation.isPaginated()) {
            effectiveOffset = offset;
            effectiveLimit = limit;
        } else {
            effectiveOffset = 0;
            effectiveLimit = ALL_ENTRIES_LIMIT;
        }

        if (request().queryString().containsKey("_query")) {
            return register.store().search(request().queryString().get("_query")[0], effectiveOffset, effectiveLimit, sortBy);
        } else {
            Map<String, String> map = request().queryString().entrySet().stream()
                    .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                    .collect(toMap(Map.Entry::getKey, queryParamEntry -> queryParamEntry.getValue()[0]));
            return register.store().search(map, effectiveOffset, effectiveLimit, sortBy);
        }

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
