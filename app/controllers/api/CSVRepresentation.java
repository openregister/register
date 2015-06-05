package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.html.Pagination;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Field;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static play.mvc.Results.ok;

public class CSVRepresentation implements Representation {
    private final Register register;
    private String separator;
    private String mediaType;

    private CSVRepresentation(Register register, String separator, String mediaType) {
        this.register = register;
        this.separator = separator;
        this.mediaType = mediaType;
    }

    public static CSVRepresentation csvInstance(Register register) {
        return new CSVRepresentation(register, ",", "text/csv; charset=utf-8");
    }

    public static CSVRepresentation tsvInstance(Register register) {
        return new CSVRepresentation(register, "\t", "text/tab-separated-values; charset=utf-8");
    }

    @Override
    public Result toListOfRecords(List<Record> records, Http.Request request, Pagination pagination) {
        return ok(header(register) + records.stream()
                        .map(this::renderRecord)
                        .collect(Collectors.joining("\n"))
        ).as(mediaType);
    }

    private String header(Register register) {
        return "hash" + separator + register.fieldNames().stream().collect(Collectors.joining(separator)) + separator + "last-updated\n";
    }

    @Override
    public Result toRecord(Record record, Http.Request request, List<RecordVersionInfo> history) {
        return ok(header(register) + renderRecord(record)).as(mediaType);
    }

    private String renderRecord(Record r) {
        return r.getHash() +
                separator +
                register.fields().stream().map(field -> renderValue(r.getEntry().get(field.getName()), field)).collect(Collectors.joining(separator)) +
                separator +
                r.getLastUpdated();
    }

    private String renderValue(JsonNode fieldValue, Field field) {
        return Optional.ofNullable(fieldValue).map(v -> Cardinality.ONE.equals(field.getCardinality()) ? encodeString(v.textValue()) : encodeArray(v)).orElse("");
    }

    public static final String QUOTE = "\"";

    private String encodeString(String v) {
        if (v.contains(QUOTE) || v.contains(separator) || v.contains("\n") || v.contains("\r"))
            return QUOTE + v + QUOTE;
        else return v;
    }

    private String encodeArray(JsonNode v) {
        return encodeString(StreamUtils.asStream(v.elements()).map(JsonNode::textValue).collect(Collectors.joining(";")));
    }

    @Override
    public boolean isPaginated() {
        return false;
    }
}
