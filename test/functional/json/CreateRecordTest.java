package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import uk.gov.openregister.JsonObjectMapper;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.Json;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.ACCEPTED;
import static play.test.Helpers.BAD_REQUEST;

@SuppressWarnings("unchecked")
public class CreateRecordTest extends ApplicationTests {

    @Test
    public void testCreateARecordReturns202() {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
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
    public void testCreateARecordWithInvalidKeysReturns400() throws JSONException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"invalidKey\": \"value1\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);

        JsonNode result = Json.parse(response.getBody());
        assertEquals(400, result.get("status").asInt());
        assertEquals("", result.get("message").textValue());
        JSONAssert.assertEquals("[{\"key\":\"invalidKey\",\"message\":\"Key not required\"}]", result.get("errors").toString(), true);

    }

    @Test
    public void testCreateARecordWithInvalidAndMissingKeysReturns400() {
        String json = "{\"name\":\"entryName\",\"invalidKey\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);

        Map<String, Object> result = JsonObjectMapper.convert(response.getBody(), Map.class);

        assertEquals(400, result.get("status"));
        assertEquals("", result.get("message"));

        List<Map> errors = (List) result.get("errors");
        assertEquals(2, errors.size());
        assertEquals(Arrays.asList("invalidKey", "test-register"), errors.stream().map(it -> it.get("key")).collect(Collectors.toList()));
    }

    @Test
    public void testCreateARecordStoresItToTheDatabase() {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";

        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        WSResponse wsResponse = getByKV("name", "entryName", "json");
        String body = wsResponse.getBody();

        JsonNode receivedEntry = Json.parse(body).get("entry");

        assertThat(receivedEntry.asText()).isEqualTo(Json.parse(json).asText());
    }

    @Test
    public void createANewRecordWithDuplicatePrimaryKeyDataReturns400() {
        String json = "{\"test-register\":\"testre'gisterkey\",\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        response = postJson("/create", json.replaceAll("value1", "newValue"));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void updatingARecordCreatesNewEntryInRegister() {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";
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
    public void updateARecordValidatesTheJson() {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        Record record = new Record(json);
        postJson("/create", json);

        String updatedJson = "{\"test-register\":\"\",\"name\":\"entryName\"}";
        WSResponse response = postJson("/supersede/" + record.getHash(), updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"message\":\"\",\"errors\":[{\"key\":\"test-register\",\"message\":\"Missing required key\"}],\"status\":400}");

    }

    @Test
    public void updateARecordReturns400Json_whenThereIsNoRecordWithTheGivenHash() {
        String updatedJson = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/supersede/nonExistingHash", updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"message\":\"No record updated\",\"errors\":[],\"status\":400}");

    }

    @Test
    public void updateARecordReturns400Json_whenTryingToUpdatePrimaryKeyColumn() {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        Record record = new Record(json);
        assertEquals(202, postJson("/create", json).getStatus());

        String updatedJson = "{\"test-register\":\"new'PrimaryKey\",\"name\":\"entryName\",\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/supersede/" + record.getHash(), updatedJson);
        assertThat(response.getBody())
                .isEqualTo("{\"message\":\"No record updated\",\"errors\":[],\"status\":400}");

    }
}