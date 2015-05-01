package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import org.junit.Ignore;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSResponse;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
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

        String updatedJson = json.replaceAll("value1", "newValue");
        WSResponse response = postJson("/supersede/" + hash, updatedJson);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        WSResponse wsResponse = getByKV("key2", "value2", "json");
        String body = wsResponse.getBody();

        JsonNode receivedEntry = Json.parse(body).get("entry");

        assertThat(receivedEntry.asText()).isEqualTo(Json.parse(updatedJson).asText());
    }


    @Test
    public void updateARecordWithMissingKeysReturns400() {
        String json = "{\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        Record record = new Record(json);
        postJson("/create", json);

        String updatedJson = "{\"name\":\"entryName\",\"key1\": \"value1\"}";
        WSResponse response = postJson("/supersede/" + record.getHash(), updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"status\":400,\"message\":\"The following keys are not allowed in the record: \"}");

    }

    @Ignore
    @Test
    public void updateShouldFailWhenHashAndPrimaryKeyDoesNotMatch() {
        throw new NotImplementedException();
    }

    //TODO: Validation message is wrong
    @Test
    public void updateARecordWithMissingValueReturns400() {
        String json = "{\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        Record record = new Record(json);
        postJson("/create", json);

        String updatedJson = "{\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"\"}";
        WSResponse response = postJson("/supersede/" + record.getHash(), updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"status\":400,\"message\":\"The following keys are not allowed in the record: \"}");

    }

    @Test
    public void updatingARecordByInvalidJsonReturns400() {
        String json = "{\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        Record record = new Record(json);
        postJson("/create", json);

        String updatedJson = "{\"invalidKey\":\"invalidValue\", \"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/supersede/" + record.getHash(), updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"status\":400,\"message\":\"The following keys are not allowed in the record: invalidKey\"}");

    }
}