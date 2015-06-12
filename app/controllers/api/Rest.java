package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.BaseController;
import controllers.api.representation.Format;
import controllers.api.representation.HtmlRepresentation;
import controllers.api.representation.JsonRepresentation;
import controllers.api.representation.Representation;
import controllers.html.Pagination;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class Rest extends BaseController {

    private static final String REPRESENTATION_QUERY_PARAM = "_representation";
    private static final int ALL_ENTRIES_LIMIT = 1000;

    @BodyParser.Of(BodyParser.Json.class)
    public Result create() throws JsonProcessingException {
        Record r = new Record(request.body().asJson());

        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.save(r);
            } catch (DatabaseException e) {
                return new HtmlRepresentation(register).toResponse(400, e.getMessage());
            }

            return new JsonRepresentation().createdResponse();
        }

        return new HtmlRepresentation(register).toResponseWithErrors(400, validationErrors);

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String hash) {
        Record r = new Record(request.body().asJson());
        List<ValidationError> validationErrors = new Validator(singletonList(register.name()), register.fieldNames()).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.update(hash, r);
            } catch (DatabaseException e) {
                return new HtmlRepresentation(register).toResponse(400, e.getMessage());
            }
            return new JsonRepresentation().createdResponse();
        }

        return new HtmlRepresentation(register).toResponseWithErrors(400, validationErrors);
    }

    public F.Promise<Result> findByKey(String key, String value, String format) {
        String canonicalUrl = routes.Rest.findByKey(key, value, "").url();
        response().setHeader("Link", "<" + canonicalUrl + ">; rel=\"canonical\"");
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByKV(key, URLDecoder.decode(value, "utf-8")));
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record -> representationFrom(format).toRecord(
                                        record,
                                        request,
                                        getHistoryFor(record)
                                )
                        ).orElse(new HtmlRepresentation(register).toResponse(404, "Entry not found")
                        )
        );
    }

    public F.Promise<Result> findByHash(String hash, String format) {
        String canonicalUrl = routes.Rest.findByHash(hash, "").url();
        response().setHeader("Link", "<" + canonicalUrl + ">; rel=\"canonical\"");
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByHash(hash));
        return recordF.map(optionalRecord ->
                        optionalRecord.map(record -> representationFrom(format).toRecord(
                                        record,
                                        request,
                                        getHistoryFor(record)
                                )
                        ).orElse(
                                new HtmlRepresentation(register).toResponse(404, "Entry not found")
                        )
        );
    }

    public F.Promise<Result> all(String format, Pager pager) throws Exception {
        return findByQuery(
                format,
                pager,
                false
        );
    }

    public F.Promise<Result> bulkDownloadInfo() throws Exception {
        return F.Promise.promise(() ->
                ok(views.html.bulkDownloadInfo.render(register, "", request)));
    }

    public F.Promise<Result> bulkDownloadTorrent() throws Exception {
        return F.Promise.promise(() ->
                status(501, views.html.notImplemented.render(register)));
    }

    public F.Promise<Result> latest(String format, Pager pager) throws Exception {
        return findByQuery(
                format,
                pager,
                true
        );
    }

    public F.Promise<Result> search(Pager pager) throws Exception {
        return findByQuery(
                request.getQueryString(REPRESENTATION_QUERY_PARAM),
                pager,
                false
        );
    }

    public F.Promise<Result> corsPreflight(String all) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        response().setHeader("Allow", "*");
        response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
        return F.Promise.pure(ok());
    }

    private F.Promise<Result> findByQuery(String format, Pager pager, boolean historic) throws Exception {
        Representation representation = representationFrom(format);

        int effectiveOffset = representation.isPaginated() ? pager.page * pager.pageSize : 0;
        int effectiveLimit = representation.isPaginated() ? pager.pageSize : ALL_ENTRIES_LIMIT;

        List<Record> records;
        int total;
        Map<String, String[]> queryParameters = request.queryString();
        if (queryParameters.containsKey("_query")) {
            records = store.search(queryParameters.get("_query")[0], 0, ALL_ENTRIES_LIMIT, historic);
            total = records.size();
        } else {
            Map<String, String> searchParamsMap = queryParameters.keySet().stream().filter(k -> !k.startsWith("_")).collect(Collectors.toMap(key -> key, key -> queryParameters.get(key)[0]));

            records = store.search(searchParamsMap, 0, ALL_ENTRIES_LIMIT, historic, queryParameters.containsKey("_exact") && queryParameters.get("_exact")[0].equals("true"));
            total = records.size();
        }

        URIBuilder uriBuilder = new URIBuilder(request.uri());

        Pagination pagination = new Pagination(uriBuilder, pager.page, total, pager.pageSize);

        return F.Promise.promise(() -> {
            if (pagination.pageDoesNotExist())
                return new HtmlRepresentation(register).toResponse(404, "Page not found");
            else return representation.toListOfRecords(
                    records.subList(effectiveOffset, (effectiveOffset + effectiveLimit < total ? effectiveOffset + effectiveLimit : total)),
                    request,
                    pagination
            );
        });
    }

    private Representation representationFrom(String format) {
        if (StringUtils.isEmpty(format)) {
            String formatQueryValue = request.getQueryString(REPRESENTATION_QUERY_PARAM);
            if (formatQueryValue == null) {
                return Format.html.createRepresentation(register);
            } else {
                return Format.representationFor(register, formatQueryValue);
            }
        } else {
            return Format.representationFor(register, format.replaceAll("\\.(.*)", "$1"));
        }
    }

    private List<RecordVersionInfo> getHistoryFor(Record r) {
        return store.previousVersions(r.getHash());
    }
}
