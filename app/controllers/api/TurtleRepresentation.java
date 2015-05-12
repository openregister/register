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
        String header = "@prefix field: <http://fields.openregister.org/field/>\n" +
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
                return jsonNode.toString();
            case MANY:
                return renderList(jsonNode);
            default:
                throw new IllegalStateException("Invalid Cardinality: " + field.getCardinality());
        }
    }

    private String renderList(JsonNode jsonNode) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        return StreamSupport.stream(arrayNode.spliterator(),false)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

    }

    public static Representation instance = new TurtleRepresentation();
}
