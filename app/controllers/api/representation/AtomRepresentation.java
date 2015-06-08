package controllers.api.representation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import controllers.html.Pagination;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joda.time.DateTime;
import play.mvc.Http;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static play.mvc.Results.ok;

public class AtomRepresentation implements Representation {
    public static final String TEXT_ATOM = "application/atom+xml; charset=utf-8";
    public static final String RFC3339_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.msZZ";
    private final CurieResolver curieResolver;
    private final Register register;

    public AtomRepresentation(Register register) {
        this.register = register;
        curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
    }

    @Override
    public Result toListOfRecords(List<Record> records, Http.Request request, Pagination pagination) {
        DateTime latestUpdate = records
                .stream()
                .map(Record::getLastUpdated)
                .sorted((dt1, dt2) -> dt1.isAfter(dt2) ? -1 : 1)
                .findFirst()
                .get();

        String entries = records.stream()
                .map(r -> renderRecord(r, register))
                .collect(Collectors.joining());

        return createAtomFeed(entries, latestUpdate);
    }

    @Override
    public Result toRecord(Record record, Http.Request request, List<RecordVersionInfo> history) {
        return createAtomFeed(renderRecord(record, register), record.getLastUpdated());
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    private Result createAtomFeed(String entries, DateTime lastUpdated) {
        return ok(createAtomHeader(lastUpdated) + entries + "</feed>").as(TEXT_ATOM);
    }

    private String createAtomHeader(DateTime dateTime) {

        String feedTemplate = "<feed xmlns:f=\"http://fields.openregister.org/field/\" xmlns:dt=\"http://fields.openregister.org/datatype/\" xmlns=\"http://www.w3.org/2005/Atom\">" +
                " <title>%s</title>" +
                " <id>%s</id>" +
                "<link rel=\"self\" href=\"%s\"/>" +
                "<updated>%s</updated>" +
                "<author><name>openregister.org</name></author>\n";

        return String.format(feedTemplate,
                register.friendlyName() + " register updates",
                curieResolver.resolve(new Curie(register.name(), "latest.atom")),
                curieResolver.resolve(new Curie(register.name(), "")),
                dateTime.toString(RFC3339_DATETIME_FORMAT));
    }

    private String renderRecord(Record record, Register register) {
        URI hashUri = curieResolver.resolve(new Curie(register.name() + "_hash", record.getHash()));

        String entryTemplate = "<entry>" +
                "<id>urn:hash:%s</id>" +
                "<title>%s</title>" +
                "<updated>%s</updated>" +
                "<author>" +
                "<name>openregister.org</name>" +
                "</author>" +
                "<link href=\"%s\"></link>" +
                "<content type=\"application/xml\">%s</content>" +
                "</entry>\n";

        String content = register.fields().stream().map(f -> String.format("<f:%s>%s</f:%s>", f.getName(), renderValue(record, f), f.getName())).collect(Collectors.joining(""));

        return String.format(entryTemplate,
                record.getHash(),
                hashUri,
                record.getLastUpdated().toString(RFC3339_DATETIME_FORMAT),
                hashUri,
                content
        );

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
}
