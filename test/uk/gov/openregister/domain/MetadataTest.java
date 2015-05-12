package uk.gov.openregister.domain;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;

public class MetadataTest {
    @Test
    public void normalise_convertsTheObjectIntoDBJson() throws JSONException {
        DateTime creationTime = DateTime.now();
        Metadata metadata = new Metadata(creationTime, "someHash");
        JSONAssert.assertEquals("{\"creationTime\":\"" + creationTime.toString() + "\",\"previousEntryHash\":\"someHash\"}", metadata.normalise(), true);
    }

    @Test
    public void from_createsMetadaObjectFromJsonString() {
        DateTime creationTime = DateTime.now();

        Metadata metadata = Metadata.from("{\"creationTime\":\"" + creationTime.toString() + "\",\"previousEntryHash\":\"someHash\"}");
        assertEquals(creationTime.toString(), metadata.creationTime.toString());
        assertEquals("someHash", metadata.previousEntryHash);
    }
}