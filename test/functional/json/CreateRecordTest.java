/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package functional.json;

import com.fasterxml.jackson.databind.JsonNode;
import functional.ApplicationTests;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSResponse;

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
}
