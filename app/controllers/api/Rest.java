package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.SortType;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record ->
                                        representationFrom(format).toRecord(
                                                record,
                                                getHistoryFor(record),
                                                representationsMap(routes.Rest.findByKey(key, value, ".__FORMAT__").url()),
                                                register()
                                        )
                        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found", register.friendlyName())
                        )
        );
    }

    public F.Promise<Result> findByHash(String hash, String format) {
        Register register = register();
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> register.store().findByHash(hash));
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record ->
                                        representationFrom(format).toRecord(
                                                record,
                                                getHistoryFor(record),
                                                //todo: . with format is required at this moment because the controller methods receives format starts with '.'
                                                representationsMap(routes.Rest.findByHash(hash, ".__FORMAT__").url()),
                                                register()
                                        )
                        ).orElse(
                                HtmlRepresentation.instance.toResponse(404, "Entry not found", register.friendlyName())
                        )
        );
    }

    final SortType.SortBy defaultSort = register().store().getSortType().getDefault();
    final SortType.SortBy lastUpdateSort = register().store().getSortType().getLastUpdate();
    public F.Promise<Result> all(String format, int page, int pageSize) throws Exception {
        return findByQuery(
                format,
                request().getQueryString("_query"),
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.all(format, p, ps).absoluteURL(request()),
                defaultSort);
    }

    public F.Promise<Result> latest(String format, int page, int pageSize) throws Exception {
        return findByQuery(
                format,
                request().getQueryString("_query"),
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.latest(format, p, ps).absoluteURL(request()),
                lastUpdateSort);
    }

    public F.Promise<Result> search(String query, int page, int pageSize) throws Exception {

        return findByQuery(
                request().getQueryString(REPRESENTATION_QUERY_PARAM),
                query,
                page,
                pageSize,
                (q, p, ps) -> controllers.api.routes.Rest.search(q, p, ps).absoluteURL(request()),
                defaultSort);
    }

    private F.Promise<Result> findByQuery(String format, String query, int page, int pageSize, PaginationUrlFunction paginationUrlFunction, SortType.SortBy sortBy) throws Exception {
        Store store = register().store();
        Representation representation = representationFrom(format);


        int effectiveOffset = representation.isPaginated() ? page * pageSize : 0;
        int effectiveLimit = representation.isPaginated() ? pageSize : ALL_ENTRIES_LIMIT;

        Map<String, String[]> queryParameters = request().queryString();

        List<Record> records;

        if (queryParameters.containsKey("_query")) {
            records = store.search(queryParameters.get("_query")[0], effectiveOffset, effectiveLimit, sortBy);
        } else {
            Map<String, String> searchParamsMap = queryParameters.keySet().stream().filter(k -> !k.startsWith("_")).collect(Collectors.toMap(key -> key, key -> queryParameters.get(key)[0]));

            records = store.search(searchParamsMap, effectiveOffset, effectiveLimit, sortBy);
        }


        String urlTemplate = new URIBuilder(request().uri()).setParameter(REPRESENTATION_QUERY_PARAM, "__FORMAT__").build().toString();

        return F.Promise.promise(() -> representation.toListOfRecords(
                records,
                representationsMap(urlTemplate),
                page > 0 ? paginationUrlFunction.apply(query, page - 1, pageSize) : null,
                records.size() == pageSize ? paginationUrlFunction.apply(query, page + 1, pageSize) : null,
                register()
        ));
    }

    @FunctionalInterface
    private interface PaginationUrlFunction {
        String apply(String query, int page, int pageSize);
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

    @SuppressWarnings("Convert2MethodRef")
    private Map<String, String> representationsMap(String urlTemplate) {
        return Stream.of(Representations.Format.values()).collect(toMap(fmt -> fmt.name(), fmt -> urlTemplate.replace("__FORMAT__", fmt.name())));
    }
}
