package helper;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.openregister.domain.Metadata;

public class DataRow {
    public String hash;
    public JsonNode entry;
    public Metadata metadata;

    public DataRow(String hash, JsonNode entry, Metadata metadata) {
        this.hash = hash;
        this.entry = entry;
        this.metadata = metadata;
    }
}
