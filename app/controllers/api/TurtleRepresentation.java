package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import controllers.App;
import play.mvc.Result;
import scala.NotImplementedError;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.model.Field;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static play.mvc.Results.ok;

// XXX this is a hacky hacky class to prove a point. It's subject to all sorts of
// data injection problems.
public class TurtleRepresentation implements Representation {
    @Override
    public Result toResponse(int status, String message) {
        // don't care about this for the moment
        return null;
    }

    @Override
    public Result toListOfRecords(List<Record> records) throws Exception {
        throw new NotImplementedError();
    }

    @Override
    public Result toRecord(Optional<Record> recordO, List<RecordVersionInfo> history) {
        return recordO.map(record -> (Result) ok(render(record, history)).as("text/turtle; charset=utf-8"))
                .orElse(toResponse(404, "Not found"));
    }

    private String render(Record record, List<RecordVersionInfo> history) {
        String header = "@prefix field: <http://fields.openregister.org/field/>.\n" +
                "\n";
        String entity = String.format("<http://%s.openregister.org/hash/%s>\n", App.instance.register.name(), record.getHash());
        String fields = App.instance.register.fields().stream()
                .map(field -> String.format("  field:%s %s", field.getName(), renderValue(record, field)))
                .collect(Collectors.joining(" ;\n", "", " .\n"));
        return header + entity + fields;
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
            String register = field.getRegister().get();
            return String.format("<http://%s.openregister.org/%s/%s>", register, register, jsonNode.asText());
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
