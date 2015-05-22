package uk.gov.openregister.domain;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetadataTest {
    @Test
    public void normalise_convertsTheObjectIntoCanonicalJson() throws JSONException {
        DateTime creationTime = DateTime.now();
        Metadata metadata = new Metadata(creationTime, "someHash");
        assertEquals("{\"creationTime\":\"" + creationTime.toString() + "\",\"previousEntryHash\":\"someHash\"}", metadata.normalise());
    }

    @Test
    public void from_createsMetadaObjectFromJsonString() {
        DateTime creationTime = DateTime.now();

        Metadata metadata = Metadata.from("{\"creationTime\":\"" + creationTime.toString() + "\",\"previousEntryHash\":\"someHash\"}");
        assertEquals(creationTime.toString(), metadata.creationTime.toString());
        assertEquals("someHash", metadata.previousEntryHash);
    }
}