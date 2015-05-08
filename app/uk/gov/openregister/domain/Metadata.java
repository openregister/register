package uk.gov.openregister.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import play.libs.Json;
import uk.gov.openregister.JsonObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
    public final DateTime creationtime;
    public final String previousEntryHash;

    public Metadata(DateTime creationtime, String previousEntryHash) {
        this.creationtime = creationtime;
        this.previousEntryHash = previousEntryHash;
    }

    public String normalise(){
        Map<String, Object> map = new HashMap<>();
        map.put("creationTime",  creationtime.toString());
        map.put("previousEntryHash",  previousEntryHash);
        return JsonObjectMapper.convertToString(map);
    }

    public static Metadata from(String metadataJson) {
        JsonNode json = Json.parse(metadataJson);
        return new Metadata(
                DateTime.parse(json.get("creationTime").textValue()),
                json.get("previousEntryHash").textValue()
        );
    }
}
