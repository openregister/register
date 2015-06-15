package uk.gov.openregister.store.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.joda.time.DateTime;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.Store;

import java.util.HashMap;
import java.util.Map;

public final class ElasticSearchImporter {
    private final ESInfo esInfo;

    public ElasticSearchImporter(Store dbStore, ESInfo esInfo) {
        this.esInfo = esInfo;
    }

    public void init() {
        //TODO
    }

    public void add(Record... records) {
        //TODO
    }

    public void add(Record record) {
        final ESRecord esRecord = new ESRecord(record, esInfo);
        final String esRepresentation = esRecord.toString();
        final String postUrl = esInfo.getUrl() + "/openregister/" + esInfo.getKey() + "/" + esRecord.keyName();
        final WSRequestHolder requestHolder = WS.url(postUrl);
        requestHolder.setContentType("application/json");
        requestHolder.put(esRepresentation);
    }

    public static final class ESRecord {
        private final Record record;
        private final ESInfo esInfo;

        public ESRecord(Record record, ESInfo esInfo) {
            this.record = record;
            this.esInfo = esInfo;
        }

        public String keyName() {
            return StreamUtils.asStream(record.getEntry().fields())
                    .filter(me -> me.getKey().equals(esInfo.getKey()))
                    .map(Map.Entry::getValue)
                    .map(JsonNode::asText)
                    .findFirst()
                    .get();
        }

        @Override
        public String toString() {
            final String hash = record.getHash();
            final JsonNode recordEntry = record.getEntry();
            final DateTime lastUpdated = record.getLastUpdated();

            final HashMap<String, JsonNode> childNodes = new HashMap<>();
            childNodes.put("hash", new TextNode(hash));
            childNodes.put("entry", recordEntry);
            childNodes.put("lastUpdated", new TextNode(lastUpdated.toString()));
            JsonNode esNode = new ObjectNode(new ObjectMapper().getNodeFactory(), childNodes);

            return esNode.asText();
        }
    }
}
