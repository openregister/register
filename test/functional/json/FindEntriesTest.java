package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.Json;
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
        JSONAssert.assertEquals(record.toString(), response.getBody(), false);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = getByHash(record.getHash(), "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        JSONAssert.assertEquals(record.toString(), response.getBody(), false);
    }


    @Test
    public void testSearch() throws Exception {
        String expectedJson1 = "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String expectedJson2 = "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry3\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        postJson("/create", expectedJson1);
        postJson("/create", expectedJson2);

        postJson("/create", "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"A\",\"B\"]}");


        WSResponse response = search("key1", "value1", "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        JsonNode result = Json.parse(response.getBody());
        assertThat(result.size()).isEqualTo(2);

        JSONAssert.assertEquals(new Record(expectedJson1).toString(), result.get(0).toString(), true);
        JSONAssert.assertEquals(new Record(expectedJson2).toString(), result.get(1).toString(), true);

    }
}
