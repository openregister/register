package uk.gov.openregister.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.Logger;
import play.libs.Json;
import uk.gov.openregister.crypto.Digest;

public class Record {

    public Record(JsonNode json) {
        this.entry = json;
        this.hash = Digest.shasum(normalise());
    }

    public Record(String jsonString){
        this(Json.parse(jsonString));
    }

    private String hash;
    private JsonNode entry;

    public String getHash() {
        return hash;
    }

    @SuppressWarnings("unused")
    public JsonNode getEntry() {
        return entry;
    }

    private static final ObjectMapper SORTED_MAPPER = new ObjectMapper();
    static {
        SORTED_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String normalise() {
        try {
            Object obj = SORTED_MAPPER.treeToValue(entry, Object.class);
            return SORTED_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Logger.warn("Unable to normalise json object, using original", e);
            return entry.toString();
        }
    }

    @Override
    public String toString() {
        try {
            return SORTED_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            Logger.warn("Unable to serialise register row, using original", e);
            return entry.toString();
        }
    }
}
