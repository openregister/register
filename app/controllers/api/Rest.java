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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class Rest extends Controller {
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
                return JsonRepresentation.instance.toResponse(400, e.getMessage());
            }

            return JsonRepresentation.instance.toResponse(202, "Record saved successfully");
        }

        return JsonRepresentation.instance.toResponseWithErrors(400, "", validationErrors);

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String hash) {
        Record r = new Record(request().body().asJson());
        List<ValidationError> validationErrors = new Validator(singletonList(registerName), fieldNames).validate(r);

        if (validationErrors.isEmpty()) {
            try {
                store.update(hash, r);
            } catch (DatabaseException e) {
                return JsonRepresentation.instance.toResponse(400, e.getMessage());
            }
            return JsonRepresentation.instance.toResponse(202, "Record saved successfully");
        }

        return JsonRepresentation.instance.toResponseWithErrors(400, "", validationErrors);
    }

    private Pair<String, Representations.Format> parseKeyAndFormat(final String keyAndFormat) {
        String[] keyAndFormatParts = keyAndFormat.split("\\.");
        if(keyAndFormatParts.length == 2) {
            return new ImmutablePair<>(keyAndFormatParts[0], Representations.Format.getFormat(keyAndFormatParts[1]));
        } else {
            return new ImmutablePair<>(keyAndFormat, Representations.Format.html);
        }
    }

    public F.Promise<Result> findByKey(String key, String value) {
        Pair<String, Representations.Format> keyAndFormat = parseKeyAndFormat(key);
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByKV(keyAndFormat.getLeft(), value));
        return recordF.map(recordO -> getResponse(recordO, keyAndFormat.getRight().representation));
    }

    public F.Promise<Result> findByHash(String hashType, String hash) {
        Pair<String, Representations.Format> keyAndFormat = parseKeyAndFormat(hashType);
        F.Promise<Optional<Record>> recordF = F.Promise.promise(() -> store.findByHash(hash));
        return recordF.map(recordO -> getResponse(recordO, keyAndFormat.getRight().representation));
    }

    private Result getResponse(Optional<Record> recordO, Representation representation) {
        return recordO.map(record -> representation.toRecord(record, getHistoryFor(record), new RepresentationParser().linksMap(request().uri())))
                .orElse(HtmlRepresentation.instance.toResponse(404, "Entry not found"));
    }

    public F.Promise<Result> search(String search) {
        Representations.Format pathFormat = parseKeyAndFormat(search).getRight();
        Optional<Representations.Format> queryParamFormatOption = new RepresentationParser().formatForQuery(request().uri());
        Representations.Format format;
        if(queryParamFormatOption.isPresent()){
            format = queryParamFormatOption.get();
        } else {
            format = pathFormat;
        }

        F.Promise<List<Record>> recordsF = F.Promise.promise(() -> {
            if (request().queryString().containsKey("_query")) {
                return store.search(request().queryString().get("_query")[0]);
            } else {
                HashMap<String, String> map = new HashMap<>();
                request().queryString().entrySet().stream()
                        .filter(queryParameter -> !queryParameter.getKey().startsWith("_"))
                        .forEach(queryParameter -> map.put(queryParameter.getKey(), queryParameter.getValue()[0]));
                return store.search(map);
            }
        });

        return recordsF.map(rs -> format.representation.toListOfRecords(rs, new RepresentationParser().linksMap(request().uri())));
    }

    private List<RecordVersionInfo> getHistoryFor(Record r) {
        return store.history(registerName, r.getEntry().get(registerName).textValue());
    }
}
