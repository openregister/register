package uk.gov.openregister.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.Logger;
import uk.gov.openregister.crypto.Digest;

public class Entry {

    public Entry(JsonNode json) {
        this.raw = sortAndConvertNodeToString(json);
        this.hash = Digest.shasum(raw);

    }


    private String raw;
    private String hash;

    public String getRaw() {
        return raw;
    }

    public String getHash() {
        return hash;
    }

    private static final ObjectMapper SORTED_MAPPER = new ObjectMapper();
    static {
        SORTED_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private String sortAndConvertNodeToString(final JsonNode node) {
        final Object obj;
        try {
            obj = SORTED_MAPPER.treeToValue(node, Object.class);
            return SORTED_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Logger.warn("Unable to normalise json object, using original", e);
            return node.toString();
        }
    }
}
