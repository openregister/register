package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Result;
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
        Record r = new Record(request.body().asJson());

        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.save(r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage(), register.friendlyName());
            }

            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors, register.friendlyName());

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String hash) {
        Record r = new Record(request.body().asJson());
        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.update(hash, r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage(), register.friendlyName());
            }
            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors, register.friendlyName());
    }

    public F.Promise<Result> findByKey(String key, String value, String format) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByKV(key, URLDecoder.decode(value, "utf-8")));
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record -> representationFrom(format).toRecord(
                                        register,
                                        record,
                                        request.queryString(),
                                        //todo: . with format is required at this moment because the controller methods receives format starts with '.'
                                        representationsMap(routes.Rest.findByKey(key, value, ".__FORMAT__").url()),
                                        getHistoryFor(record)
                                )
                        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found", register.friendlyName())
                        )
        );
    }

    public F.Promise<Result> findByHash(String hash, String format) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByHash(hash));
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record -> representationFrom(format).toRecord(
                                        register,
                                        record,
                                        request.queryString(),
                                        //todo: . with format is required at this moment because the controller methods receives format starts with '.'
                                        representationsMap(routes.Rest.findByHash(hash, ".__FORMAT__").url()),
                                        getHistoryFor(record)
                                )
                        ).orElse(
                                HtmlRepresentation.instance.toResponse(404, "Entry not found", register.friendlyName())
                        )
        );
    }

    public F.Promise<Result> all(String format, Pager pager) throws Exception {
        return findByQuery(
                format,
                pager,
                store.getSortType().getDefault());
    }

    public F.Promise<Result> latest(String format, Pager pager) throws Exception {
        return findByQuery(
                format,
                pager,
                store.getSortType().getLastUpdate());
    }

    public F.Promise<Result> search(Pager pager) throws Exception {
        return findByQuery(
                request.getQueryString(REPRESENTATION_QUERY_PARAM),
                pager,
                store.getSortType().getDefault());
    }

    private F.Promise<Result> findByQuery(String format, Pager pager, SortType.SortBy sortBy) throws Exception {
        Representation representation = representationFrom(format);

        int effectiveOffset = representation.isPaginated() ? pager.page * pager.pageSize : 0;
        int effectiveLimit = representation.isPaginated() ? pager.pageSize : ALL_ENTRIES_LIMIT;

        List<Record> records;

        Map<String, String[]> queryParameters = request.queryString();
        if (queryParameters.containsKey("_query")) {
            records = store.search(queryParameters.get("_query")[0], effectiveOffset, effectiveLimit, sortBy);
        } else {
            Map<String, String> searchParamsMap = queryParameters.keySet().stream().filter(k -> !k.startsWith("_")).collect(Collectors.toMap(key -> key, key -> queryParameters.get(key)[0]));

            records = store.search(searchParamsMap, effectiveOffset, effectiveLimit, sortBy);
        }

        URIBuilder uriBuilder = new URIBuilder(request.uri());

        String urlTemplate = new URIBuilder(uriBuilder.build()).setParameter(REPRESENTATION_QUERY_PARAM, "__FORMAT__").build().toString();

        return F.Promise.promise(() -> representation.toListOfRecords(
                register, records,
                queryParameters,
                representationsMap(urlTemplate),
                pager.page > 0 ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (pager.page - 1)).build().toString() : null,
                records.size() == pager.pageSize ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (pager.page + 1)).build().toString() : null
        ));
    }

    private Representation representationFrom(String format) {
        if (StringUtils.isEmpty(format)) {
            String representationQueryValue = request.getQueryString(REPRESENTATION_QUERY_PARAM);
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
        return store.previousVersions(r.getHash());
    }

    @SuppressWarnings("Convert2MethodRef")
    private Map<String, String> representationsMap(String urlTemplate) {
        return Stream.of(Representations.Format.values()).collect(toMap(fmt -> fmt.name(), fmt -> urlTemplate.replace("__FORMAT__", fmt.name())));
    }
}
