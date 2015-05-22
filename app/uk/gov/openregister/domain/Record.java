package uk.gov.openregister.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.crypto.Digest;

import java.util.Map;
import java.util.Optional;

public class Record {
    public Record(JsonNode json) {
        this(json, Optional.empty());
    }

    public Record(JsonNode json, Optional<Metadata> metadata) {
        this.entry = json;
        this.hash = Digest.shasum(normalisedEntry());
        this.metadata = metadata;
    }

    public Record(String jsonString) {
        this(Json.parse(jsonString));
    }

    private String hash;
    private JsonNode entry;

    public Optional<Metadata> getMetadata() {
        return metadata;
    }

    @JsonIgnore
    private Optional<Metadata> metadata;

    public String getHash() {
        return hash;
    }

    @SuppressWarnings("unused")
    public JsonNode getEntry() {
        return entry;
    }

    @JsonProperty("lastUpdated")
    public String getLastUpdated() {
        return metadata.map(m -> m.creationTime.toString()).orElse("");
    }

    public String normalisedEntry() {
        //noinspection unchecked
        return JsonObjectMapper.convertToString(JsonObjectMapper.convert(entry, Map.class));
    }
}
