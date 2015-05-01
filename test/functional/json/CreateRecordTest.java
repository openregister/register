package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.ACCEPTED;
import static play.test.Helpers.BAD_REQUEST;

public class CreateRecordTest extends ApplicationTests {

    @Test
    public void testCreateARecordReturns202() {
        String json = "{\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);
    }

    @Test
    public void testCreateARecordWithMalformedRequestReturns400() {
        String json = "this is not json";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testCreateARecordWithInvalidKeysReturns400() {
        String json = "{\"name\":\"entryName\",\"invalidKey\": \"value1\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);

        assertThat(response.getBody())
                .isEqualTo("{\"status\":400,\"message\":\"The following keys are not allowed in the record: invalidKey\"}");

    }

    @Test
    public void testCreateARecordWithInvalidAndMissingKeysReturns400() {
        String json = "{\"name\":\"entryName\",\"invalidKey\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);

        assertThat(response.getBody())
                .isEqualTo("{\"status\":400,\"message\":\"The following keys are not allowed in the record: invalidKey\"}");

    }

    @Test
    public void testCreateARecordStoresItToTheDatabase() {
        String json = "{\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";

        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        WSResponse wsResponse = getByKV("name", "entryName", "json");
        String body = wsResponse.getBody();

        JsonNode receivedEntry = Json.parse(body).get("entry");

        assertThat(receivedEntry.asText()).isEqualTo(Json.parse(json).asText());
    }

    @Test
    public void updatingARecordCreatesNewEntryInRegister() {
        String json = "{\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";
        String hash = new Record(json).getHash();
        postJson("/create", json);

        String updatedJson = json.replaceAll("entryName", "newEntryName");
        WSResponse response = postJson("/supersede/" + hash, updatedJson);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        WSResponse wsResponse = getByKV("name", "entryName", "json");
        String body = wsResponse.getBody();

        JsonNode receivedEntry = Json.parse(body).get("entry");

        assertThat(receivedEntry.asText()).isEqualTo(Json.parse(updatedJson).asText());
    }
}