package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.App;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.DatabaseException;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.validation.ValidationError;
import uk.gov.openregister.validation.Validator;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static controllers.api.Representations.Format.html;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

public class Rest extends Controller {

    private static final String REPRESENTATION_QUERY_PARAM = "_representation";
    public static final int DEFAULT_RESULT_PAGE_SIZE = 30;
    private static final int ALL_ENTRIES_LIMIT = 1000;

    private final Store store;
    private final List<String> fieldNames;
    private final String registerName;

    public Rest() {
        store = App.instance.register.store();
        fieldNames = App.instance.register.fieldNames();
        registerName = App.instance.register.name();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result create() throws JsonProcessingException {
        Record r = new Record(request().body().asJson());

        List<ValidationError> validationErrors = new Validator(singletonList(registerName), fieldNames).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.save(r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage());
            }

            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors);

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String hash) {
        Record r = new Record(request().body().asJson());
        List<ValidationError> validationErrors = new Validator(singletonList(registerName), fieldNames).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.update(hash, r);
            } catch (DatabaseException e) {
                return HtmlRepresentation.instance.toResponse(400, e.getMessage());
            }
            return JsonRepresentation.instance.createdResponse();
        }

        return HtmlRepresentation.instance.toResponseWithErrors(400, validationErrors);
    }

    public F.Promise<Result> findByKey(String key, String value) {
        return findByKeyWithFormat(key, value, representationQueryString());
    }

    public F.Promise<Result> findByKeyWithFormat(String key, String value, String format) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByKV(key, URLDecoder.decode(value, "utf-8")));
        return recordF.map(record -> getResponse(record, format,
                anyFormat -> controllers.api.routes.Rest.findByKeyWithFormat(key, value, anyFormat).url()));
    }

    public F.Promise<Result> findByHash(String hash) {
        return findByHashWithFormat(hash, representationQueryString());
    }

    public F.Promise<Result> findByHashWithFormat(String hash, String format) {
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByHash(hash));
        return recordF.map(record -> getResponse(record, format,
                anyFormat -> controllers.api.routes.Rest.findByHashWithFormat(hash, anyFormat).url()));
    }

    private String representationQueryString() {
        return request().getQueryString(REPRESENTATION_QUERY_PARAM);
    }

    private Result getResponse(Optional<Record> recordO, String format, Function<String, String> routeForFormat) {
        final Representation representation;
        try {
            representation = representationFor(format);
        } catch (IllegalArgumentException e) {
            return formatNotRecognisedResponse(format);
        }
        return recordO.map(record ->
                        representation.toRecord(record, getHistoryFor(record), representationsMap(routeForFormat))
        ).orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found"));
    }

    public F.Promise<Result> all(int page, int pageSize) throws Exception {
        Pair<Integer, Integer> offsetAndLimitForNextPage = offsetAndLimitForNextPage(page, pageSize);
        Representation representation = html.representation;

        return doSearch(offsetAndLimitForNextPage.getLeft(), offsetAndLimitForNextPage.getRight(), representation)
                .map(rs ->
                        representation.toListOfRecords(rs, representationsMap(this::searchUriForFormat),
                                pageLinksMap(page > 0 ?
                                                Optional.of(controllers.api.routes.Rest.all(page - 1, pageSize).absoluteURL(request()))
                                                : Optional.empty(),
                                        rs.size() == pageSize ?
                                                Optional.of(controllers.api.routes.Rest.all(page + 1, pageSize).absoluteURL(request()))
                                                : Optional.empty()
                                )));
    }

    public F.Promise<Result> allWithFormat(String format, int page, int pageSize) throws Exception {
        Pair<Integer, Integer> offsetAndLimitForNextPage = offsetAndLimitForNextPage(page, pageSize);
        Representation representation = representationFor(format);

        return doSearch(offsetAndLimitForNextPage.getLeft(), offsetAndLimitForNextPage.getRight(), representation)
                .map(rs ->
                        representation.toListOfRecords(rs, representationsMap(this::searchUriForFormat),
                                pageLinksMap(page > 0 ?
                                                Optional.of(controllers.api.routes.Rest.allWithFormat(format, page - 1, pageSize).absoluteURL(request()))
                                                : Optional.empty(),
                                        rs.size() == pageSize ?
                                                Optional.of(controllers.api.routes.Rest.allWithFormat(format, page + 1, pageSize).absoluteURL(request()))
                                                : Optional.empty()
                                )));
    }

    public F.Promise<Result> search(String query, int page, int pageSize) throws Exception {
        Pair<Integer, Integer> offsetAndLimitForNextPage = offsetAndLimitForNextPage(page, pageSize);
        Representation representation = representationFor(null);

        return doSearch(offsetAndLimitForNextPage.getLeft(), offsetAndLimitForNextPage.getRight(), representation).map(rs ->
                representation.toListOfRecords(rs, representationsMap(this::searchUriForFormat),
                        pageLinksMap(page > 0 ?
                                        Optional.of(controllers.api.routes.Rest.search(query, page - 1, pageSize).absoluteURL(request()))
                                        : Optional.empty(),
                                rs.size() == pageSize ?
                                        Optional.of(controllers.api.routes.Rest.search(query, page+1, pageSize).absoluteURL(request()))
                                        : Optional.empty()
                        )));

    }

    private Representation representationFor(String format) {
        if(format == null)
            return Representations.representationFor(representationQueryString());

        return Representations.representationFor(format);
    }

    private F.Promise<List<Record>> doSearch(int offset, int limit, Representation representation) throws Exception {
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
                return store.search(request().queryString().get("_query")[0], effectiveOffset, effectiveLimit);
            } else {
                Map<String, String> map = request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .collect(toMap(Map.Entry::getKey, queryParamEntry -> queryParamEntry.getValue()[0]));
                return store.search(map, effectiveOffset, effectiveLimit);
            }
        });
    }

    private Map<String, String> pageLinksMap(Optional<String> previousPageLink, Optional<String> nextPageLink) {
        final Map<String, String> pageLinksMap = new HashMap<>();

        if (previousPageLink.isPresent()) {
            pageLinksMap.put("previous_page", previousPageLink.get());
        }

        if (nextPageLink.isPresent()) {
            pageLinksMap.put("next_page", nextPageLink.get());
        }

        return pageLinksMap;
    }

    private Result formatNotRecognisedResponse(String format) {
        return HtmlRepresentation.instance.toResponse(400, "Format '" + format + "' not recognised");
    }

    private List<RecordVersionInfo> getHistoryFor(Record r) {
        return store.previousVersions(r.getHash());
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

    private Pair<Integer, Integer> offsetAndLimitForNextPage(int currentPage, int pageSize) {
        int offset = currentPage * pageSize;

        return new ImmutablePair<>(offset, pageSize);
    }
}
