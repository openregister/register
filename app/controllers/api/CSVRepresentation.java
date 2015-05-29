package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Result;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Field;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static play.mvc.Results.ok;

public class CSVRepresentation implements Representation {

    public static CSVRepresentation csvInstance = new CSVRepresentation(",");
    public static CSVRepresentation tsvInstance = new CSVRepresentation("\t");

    private static final String MEDIA_TYPE = "text/turtle; charset=utf-8";
    private String separator;

    public CSVRepresentation(String separator) {
        this.separator = separator;
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap, String previousPageLink, String nextPageLink, Register register) throws Exception {
        return ok(header(register) + records.stream()
                        .map(r -> renderRecord(r, register))
                        .collect(Collectors.joining("\n"))
        ).as(MEDIA_TYPE);
    }

    private String header(Register register) {
        return "hash" + separator + register.fieldNames().stream().collect(Collectors.joining(separator)) + separator + "last-updated\n";
    }

    @Override
    public Result toRecord(Record record, List<RecordVersionInfo> history, Map<String, String> representationsMap, Register register) {
        return ok(header(register) + renderRecord(record, register)).as(MEDIA_TYPE);
    }

    private String renderRecord(Record r, Register register) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getHash());
        sb.append(separator);
        sb.append(register.fields().stream()
                .map(field -> renderValue(r.getEntry().get(field.getName()), field))
                .collect(Collectors.joining(separator)));
        sb.append(separator);
        sb.append(r.getLastUpdated());
        return sb.toString();
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
