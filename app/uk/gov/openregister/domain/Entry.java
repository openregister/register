package uk.gov.openregister.domain;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.openregister.crypto.Digest;

public class Entry {

    public Entry(JsonNode json) {
        // TODO normalise
        this.raw = json.toString();
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
}
