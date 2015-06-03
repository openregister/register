package uk.gov.openregister.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.libs.Json;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.crypto.Digest;

import java.util.Map;

public class Record {
    public Record(JsonNode json) {
        this(json, DateTime.now(DateTimeZone.UTC));
    }

    public Record(JsonNode json, DateTime lastUpdated) {
        this.entry = json;
        this.hash = Digest.shasum(normalisedEntry());
        this.lastUpdated = lastUpdated;
    }

    public Record(String jsonString) {
        this(Json.parse(jsonString));
    }

    private String hash;
    private JsonNode entry;
    @JsonProperty("last-updated")
    private DateTime lastUpdated;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String previousEntryHash;

    public String getPreviousEntryHash() {
        return previousEntryHash;
    }

    public void setPreviousEntryHash(String previousEntryHash) {
        this.previousEntryHash = previousEntryHash;
    }

    public DateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getHash() {
        return hash;
    }

    @SuppressWarnings("unused")
    public JsonNode getEntry() {
        return entry;
    }

    public String normalisedEntry() {
        //noinspection unchecked
        return JsonObjectMapper.convertToString(JsonObjectMapper.convert(entry, Map.class));
    }
}
