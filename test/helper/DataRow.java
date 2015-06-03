package helper;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;

public class DataRow {
    public String hash;
    public JsonNode entry;
    public DateTime lastUpdated;
    public String previousHash;

    public DataRow(String hash, JsonNode entry, DateTime lastUpdated, String previousHash) {
        this.hash = hash;
        this.entry = entry;
        this.lastUpdated = lastUpdated;
        this.previousHash = previousHash;
    }

}
