package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joda.time.DateTime;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static play.mvc.Results.ok;

public class AtomRepresentation implements Representation {
    public static final String FIELD_REGISTER_NAMESPACE_PREFIX = "f";
    public static final String DATATYPE_REGISTER_NAMESPACE_PREFIX = "dt";
    public static final String FIELD_REGISTER_NAMESPACE =
            "xmlns:" + FIELD_REGISTER_NAMESPACE_PREFIX + "=\"http://fields.openregister.org/field/\"";
    public static final String DATATYPE_REGISTER_NAMESPACE =
            "xmlns:" + DATATYPE_REGISTER_NAMESPACE_PREFIX + "=\"http://fields.openregister.org/datatype/\"";

    public static final String TEXT_ATOM = "application/atom+xml; charset=utf-8";
    public static final String RFC3339_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.msZZ";
    private final CurieResolver curieResolver;

    public AtomRepresentation() {
        curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
    }

    @Override
    public Result toListOfRecords(Register register, List<Record> records, Map<String, String[]> requestParams, Map<String, String> representationsMap, String previousPageLink, String nextPageLink) {
        String atomHeader = createAtomHeader(register, records);
        String entries = records.stream()
                .map(r -> renderRecord(r, register))
                .collect(Collectors.joining());
        String atomFooter = createAtomFooter();
        return ok(atomHeader + entries + atomFooter).as(TEXT_ATOM);
    }

    @Override
    public Result toRecord(Register register, Record record, Map<String, String[]> requestParams, Map<String, String> representationsMap, List<RecordVersionInfo> history) {
        String atomHeader = createAtomHeader(register, Collections.singletonList(record));
        String entry = renderRecord(record, register);
        String atomFooter = createAtomFooter();

        return ok(atomHeader + entry + atomFooter).as(TEXT_ATOM);
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    private String createAtomHeader(Register register, List<Record> records) {
        DateTime mostRecentlyUpdated = mostRecentlyUpdated(records);

        return "<feed " + FIELD_REGISTER_NAMESPACE + "\n" +
                " " + DATATYPE_REGISTER_NAMESPACE + "\n" +
        " xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                " <title>" + register.friendlyName() + " register updates</title>\n" +
                " <id>" + curieResolver.resolve(new Curie(register.name(), "latest.atom")) + "</id>\n" +
                "<link rel=\"self\" href=\"" + curieResolver.resolve(new Curie(register.name(), "")) + "\" />\n" +
                "<updated>" + mostRecentlyUpdated.toString(RFC3339_DATETIME_FORMAT) + "</updated>\n" +
                "<author><name>openregister.org</name></author>\n";
    }

    private DateTime mostRecentlyUpdated(List<Record> records) {
        Optional<Record> mostRecentlyUpdatedRecordO = records.stream()
                .max((a, b) ->
                        Long.valueOf(a.getMetadata().get().creationTime.getMillis() - b.getMetadata().get().creationTime.getMillis()).intValue());
        if(mostRecentlyUpdatedRecordO.isPresent()){
            return mostRecentlyUpdatedRecordO.get().getMetadata().get().creationTime;
        }

        // Every record should have a metadata with a creationtime - if not something bad has happened and we shouldnt carry on.
        return null;
    }

    private String createAtomFooter() {
        return "</feed>";
    }

    private String renderRecord(Record record, Register register) {
        URI hashUri = curieResolver.resolve(new Curie(register.name() + "_hash", record.getHash()));

        Map<String, String> keyValue = register.fields().stream()
                .map(field -> new HashMap.SimpleEntry<String, String>(field.getName(), renderValue(record, field)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String entry = "<entry>\n";
        entry += "<id>urn:hash:" + record.getHash() + "</id>\n";
        entry += "<title>" + hashUri.toString() + "</title>\n";
        entry += "<updated>" + renderCreationTime(record) + "</updated>\n";
        entry += "<author><name>openregister.org</name></author>\n";
        entry += "<link href=\"" + hashUri.toString() + "\"></link>";
        entry += "<content type=\"application/xml\">\n" + fieldEntriesForRecord(register, record) + "</content>\n";
        entry += "</entry>\n";

        return entry;
    }

    private String fieldEntriesForRecord(Register register, Record record) {
        return register.fields().stream()
                .map(f -> {
                    String fieldOpenTag = "<" + FIELD_REGISTER_NAMESPACE_PREFIX + ":" + f.getName() + ">";
                    String fieldCloseTag = "</" + FIELD_REGISTER_NAMESPACE_PREFIX + ":" + f.getName() + ">";
                    String fieldValue = renderValue(record, f);
                    return fieldOpenTag + fieldValue + fieldCloseTag;
                })
                .collect(Collectors.joining("\n")) + "\n";
    }


    private String renderValue(Record record, Field field) {
        JsonNode jsonNode = record.getEntry().get(field.getName());
        if (jsonNode == null) {
            return "\"\"";
        }
        switch (field.getCardinality()) {
            case ONE:
                return renderScalar(field, jsonNode);
            case MANY:
                return renderList(field, jsonNode);
            default:
                throw new IllegalStateException("Invalid Cardinality: " + field.getCardinality());
        }
    }

    private String renderScalar(Field field, JsonNode jsonNode) {
        if (field.getRegister().isPresent()) {
            Curie curie = new Curie(field.getRegister().get(), jsonNode.asText());
            return String.format("%s", curieResolver.resolve(curie));
        }
        return StringEscapeUtils.escapeXml(jsonNode.asText());
    }

    private String renderList(Field field, JsonNode jsonNode) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(val -> renderScalar(field, val))
                .collect(Collectors.joining(", "));
    }

    private String renderCreationTime(Record record) {
        return record.getMetadata().isPresent()
                ? record.getMetadata().get().creationTime.toString(RFC3339_DATETIME_FORMAT)
                : null;
    }

    public static Representation instance = new AtomRepresentation();
}
