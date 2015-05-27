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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static controllers.api.Representations.Format.html;
import static controllers.api.Representations.representationFor;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

public class Rest extends Controller {

    private static final String REPRESENTATION_QUERY_PARAM = "_representation";
    private static final String LIMIT_QUERY_PARAM = "_limit";
    private static final int DEFAULT_RESULT_PAGE_SIZE = 100;
    private static final int ALL_ENTRIES_LIMIT = 100000;

    private static final String PAGE_SIZE = "_page_size";
    private static final String PAGE = "_page";

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

    private int limitQueryValue() {
        try {
            return Integer.parseInt(request().getQueryString(LIMIT_QUERY_PARAM));
        } catch (NullPointerException | NumberFormatException e) {
            return DEFAULT_RESULT_PAGE_SIZE;
        }
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

    public F.Promise<Result> all() throws Exception {
        return allWithFormat(html.name());
    }

    public F.Promise<Result> allWithFormat(String format) throws Exception {
        String uri = request().uri();
        int currentPage = searchUriForCurrentPage(uri);
        Pair<Integer, Integer> offsetAndLimitForNextPage = offsetAndLimitForNextPage(currentPage, searchUriForPageSize(uri));
        return doSearch(currentPage, offsetAndLimitForNextPage.getLeft(), offsetAndLimitForNextPage.getRight(), Optional.of(format));
    }

    public F.Promise<Result> search() throws Exception {
        String uri = request().uri();
        int currentPage = searchUriForCurrentPage(uri);
        Pair<Integer, Integer> offsetAndLimitForNextPage = offsetAndLimitForNextPage(currentPage, searchUriForPageSize(uri));

        return doSearch(currentPage, offsetAndLimitForNextPage.getLeft(), offsetAndLimitForNextPage.getRight(), Optional.empty());
    }

    private F.Promise<Result> doSearch(int page, int offset, int limit, Optional<String> format) throws Exception {
        Representation representation;
        try {
            representation = format.map(Representations::representationFor)
                    .orElse(representationFor(representationQueryString()));
        } catch (IllegalArgumentException e) {
            return F.Promise.pure(formatNotRecognisedResponse(representationQueryString()));
        }

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return store.search(request().queryString().get("_query")[0], offset, limit);
            } else {
                Map<String, String> map = request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .collect(toMap(Map.Entry::getKey, queryParamEntry -> queryParamEntry.getValue()[0]));
                return store.search(map, offset, limit);
            }
        });

        return recordsF.map(rs ->
                representation.toListOfRecords(rs, representationsMap(this::searchUriForFormat),
                        pageLinksMap(page, offset, limit, rs.size())));
    }

    private Map<String, String> pageLinksMap(int page, int offset, int limit, int resultSize) {
        final Map<String, String> pageLinksMap = new HashMap<>();

        if(page >= 1) {
            pageLinksMap.put("previous_page", "_page=" + (page - 1));
        }

        if(resultSize == limit) {
            pageLinksMap.put("next_page", "_page=" + (page + 1));
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

    private final static Pattern currentPagePattern = Pattern.compile(PAGE + "=(\\d+)");
    private final static Pattern pageSizePattern = Pattern.compile(PAGE_SIZE + "=(\\d+)");
    private int searchUriForPageSize(String uriString){
        int pageSize = DEFAULT_RESULT_PAGE_SIZE;

        if(uriString.contains(PAGE_SIZE)) {
            Matcher matcher;
            if((matcher = pageSizePattern.matcher(uriString)).find()) {
                String pageSizeStr = matcher.group(1);
                pageSize = Integer.parseInt(pageSizeStr);
            }
        }

        return pageSize;
    }

    private int searchUriForCurrentPage(String uriString){
        int currentPage = 0;

        if(uriString.contains(PAGE)) {
            Matcher matcher;
            if((matcher = currentPagePattern.matcher(uriString)).find()) {
                String currentPageStr = matcher.group(1);
                currentPage = Integer.parseInt(currentPageStr);
            }
        }

        return currentPage;
    }

    private Pair<Integer, Integer> offsetAndLimitForNextPage(int currentPage, int pageSize) {
        int offset = currentPage * pageSize;
        int limit = pageSize;

        return new ImmutablePair<Integer, Integer>(offset, limit);
    }
}
