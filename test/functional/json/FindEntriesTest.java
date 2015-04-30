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
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.Json;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class FindEntriesTest extends ApplicationTests {

    @Test
    public void testFindOneByKey() throws Exception {
        String json = "{\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = getByKV("key1", "value1", "json");
        assertThat(response.getStatus()).isEqualTo(OK);
        JSONAssert.assertEquals(record.toString(), response.getBody(), false);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        String json = "{\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = getByHash(record.getHash(), "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        JSONAssert.assertEquals(record.toString(), response.getBody(), false);
    }


    @Test
    public void testSearch() throws Exception {
        String expectedJson1 = "{\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String expectedJson2 = "{\"name\":\"The Entry3\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        postJson("/create", expectedJson1);
        postJson("/create", expectedJson2);

        postJson("/create", "{\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"A\",\"B\"]}");


        WSResponse response = search("key1", "value1", "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        JsonNode result = Json.parse(response.getBody());
        assertThat(result.size()).isEqualTo(2);

        JSONAssert.assertEquals(new Record(expectedJson1).toString(), result.get(0).toString(), true);
        JSONAssert.assertEquals(new Record(expectedJson2).toString(), result.get(1).toString(), true);

    }
}
