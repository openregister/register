package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static play.mvc.Results.ok;

// XXX this is a hacky hacky class to prove a point. It's subject to all sorts of
// data injection problems.
public class TurtleRepresentation implements Representation {

    public static final String TURTLE_HEADER = "@prefix field: <http://fields.openregister.org/field/>.\n" +
            "\n";
    public static final String TEXT_TURTLE = "text/turtle; charset=utf-8";
    private final CurieResolver curieResolver;

    public TurtleRepresentation() {
        curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap, String previousPageLink, String nextPageLink, Register register) throws Exception {
        return ok(records.stream()
                        .map(r -> renderRecord(r, register))
                        .collect(Collectors.joining("\n", TURTLE_HEADER, ""))
        ).as(TEXT_TURTLE);
    }

    @Override
    public Result toRecord(Record record, List<RecordVersionInfo> history, Map<String, String> representationsMap, Register register) {
        return ok(TURTLE_HEADER + renderRecord(record, register)).as(TEXT_TURTLE);
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    private String renderRecord(Record record, Register register) {
        URI hashUri = curieResolver.resolve(new Curie(register.name() + "_hash", record.getHash()));
        String entity = String.format("<%s>\n", hashUri);
        return register.fields().stream()
                .map(field -> String.format("  field:%s %s", field.getName(), renderValue(record, field)))
                .collect(Collectors.joining(" ;\n", entity, " .\n"));
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
            return String.format("<%s>", curieResolver.resolve(curie));
        }
        return jsonNode.toString();
    }

    private String renderList(Field field, JsonNode jsonNode) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        return StreamSupport.stream(arrayNode.spliterator(),false)
                .map(val -> renderScalar(field, val))
                .collect(Collectors.joining(", "));

    }

    public static Representation instance = new TurtleRepresentation();
}
