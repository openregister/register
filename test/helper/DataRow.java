package helper;

import com.fasterxml.jackson.databind.JsonNode;

public class DataRow {
    public String hash;
    public JsonNode entry;
//    public JsonNode metadata;

    public DataRow(String hash, JsonNode entry/*, JsonNode metadata*/) {
        this.hash = hash;
        this.entry = entry;
//        this.metadata = metadata;
    }
}
