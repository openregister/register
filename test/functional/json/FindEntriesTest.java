package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class FindEntriesTest extends ApplicationTests {

    @Test
    public void testFindOneByKey() throws Exception {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = getByKV("key1", "value1", "json");
        assertThat(response.getStatus()).isEqualTo(OK);
        String expectedLinkHeaderContents = "</key1/value1>; rel=\"canonical\"";
        assertThat(response.getAllHeaders().get("Link")).contains(expectedLinkHeaderContents);

        final JsonNode responseJson = response.asJson();

        checkResponseRecords(record, responseJson);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = getByHash(record.getHash(), "json");
        assertThat(response.getStatus()).isEqualTo(OK);
        String expectedLinkHeaderContents = "</hash/" + record.getHash() + ">; rel=\"canonical\"";
        assertThat(response.getAllHeaders().get("Link")).contains(expectedLinkHeaderContents);

        final JsonNode responseJson = response.asJson();

        checkResponseRecords(record, responseJson);
    }

    @Ignore("This needs some thought as the ordering has now been switched off")
    public void testSearch() throws Exception {
        String expectedJson1 = "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String expectedJson2 = "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry3\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        postJson("/create", expectedJson2);
        postJson("/create", expectedJson1);

        postJson("/create", "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"A\",\"B\"]}");

        final Record record1 = new Record(expectedJson1);
        final Record record2 = new Record(expectedJson2);

        WSResponse response = search("key1", "value1", "json");
        assertThat(response.getStatus()).isEqualTo(OK);

        JsonNode result = response.asJson();
        assertThat(result.size()).isEqualTo(2);

        checkResponseRecords(record1, result.get(0));
        checkResponseRecords(record2, result.get(1));
    }

    private void checkResponseRecords(Record record, JsonNode responseJson) throws JSONException {
        final JsonNode recordEntry = responseJson.get("entry");
        final String lastUpdatedEntry = responseJson.get("last-updated").textValue();
        Record actual = new Record(recordEntry);

        JSONAssert.assertEquals(record.getEntry().toString(), actual.getEntry().toString(), false);
        assertThat(lastUpdatedEntry).isNotEmpty();

        final DateTime lastUpdatedDateTime = DateTime.parse(lastUpdatedEntry);
        assertThat(lastUpdatedDateTime).isNotNull();
    }
}
