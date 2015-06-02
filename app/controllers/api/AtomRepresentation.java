package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import org.joda.time.DateTime;
import play.mvc.Result;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static play.mvc.Results.ok;

public class AtomRepresentation implements Representation {
    public static final String TEXT_ATOM = "application/atom+xml; charset=utf-8";
    private final CurieResolver curieResolver;

    public AtomRepresentation() {
        curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
    }

    @Override
    public Result toListOfRecords(Register register, List<Record> records, Map<String, String[]> requestParams, Map<String, String> representationsMap, String previousPageLink, String nextPageLink) {
        SyndFeed atomFeed = createSyndFeed(register);

        atomFeed.setEntries(
                records.stream()
                        .map(r -> renderRecord(r, register))
                        .collect(Collectors.toList()));

        return ok(toOutput(atomFeed)).as(TEXT_ATOM);
    }

    @Override
    public Result toRecord(Register register, Record record, Map<String, String[]> requestParams, Map<String, String> representationsMap, List<RecordVersionInfo> history) {
        SyndFeed atomFeed = createSyndFeed(register);

        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        entries.add(renderRecord(record, register));

        atomFeed.setEntries(entries);
        return ok(toOutput(atomFeed)).as(TEXT_ATOM);
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    private SyndFeed createSyndFeed(Register register) {
        SyndFeed atomFeed = new SyndFeedImpl();
        atomFeed.setFeedType("atom_1.0");

        atomFeed.setTitle("Latest updates to the " + register.friendlyName() + " register.");
        atomFeed.setLink("http://" + register.name() + ".openregister.org");
        atomFeed.setDescription("This feed contains that latest updates to data in the " + register.friendlyName() + " register.");

        return atomFeed;
    }

    private String toOutput(final SyndFeed feed) {
        try {
            Writer writer = new StringWriter();
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);

            return writer.toString();
        } catch (IOException|FeedException e) {
            e.printStackTrace();
        }

        return "";
    }

    private SyndEntry renderRecord(Record record, Register register) {
        URI hashUri = curieResolver.resolve(new Curie(register.name() + "_hash", record.getHash()));

        SyndEntry entry = new SyndEntryImpl();

        Map<String, String> keyValue = register.fields().stream()
                .map(field -> new HashMap.SimpleEntry<String, String>(field.getName(), renderValue(record, field)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        entry.setTitle(keyValue.get("name"));
        entry.setLink(hashUri.toString());
        DateTime publishedDateTime = renderCreationTime(record);
        entry.setPublishedDate(publishedDateTime.toDate());
        SyndContent description = new SyndContentImpl();
        description.setType("text/plain");
        description.setValue("This " + register.name() + " was updated on " + friendlyFormatDateTime(renderCreationTime(record)));
        entry.setDescription(description);

        return entry;
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

    private DateTime renderCreationTime(Record record) {
        return record.getMetadata().isPresent() ? record.getMetadata().get().creationTime : null;
    }

    private static final String FRIENDLY_DATETIME_PATTERN = "yyyy-MM-dd 'at' HH:mm";
    private String friendlyFormatDateTime(DateTime dateTime) {
        return dateTime.toString(FRIENDLY_DATETIME_PATTERN);
    }

    public static Representation instance = new AtomRepresentation();
}
