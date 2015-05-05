package uk.gov.openregister.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import play.libs.Json;

public class Metadata {
    public final DateTime creationtime;
    public final String previousEntryHash;

    public Metadata(DateTime creationtime, String previousEntryHash) {
        this.creationtime = creationtime;
        this.previousEntryHash = previousEntryHash;
    }

    public String normalise(){
        return String.format("{\"creationTime\" : \"%s\", \"previousEntryHash\":\"%s\"}", creationtime.toString(), previousEntryHash);
    }

    public static Metadata from(String metadataJson) {
        JsonNode json = Json.parse(metadataJson);
        return new Metadata(
                DateTime.parse(json.get("creationTime").textValue()),
                json.get("previousEntryHash").textValue()
        );
    }
}
