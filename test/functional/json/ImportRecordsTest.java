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
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import helper.PostgresqlStoreForTesting;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class ImportRecordsTest extends ApplicationTests {


    @Test
    public void testImportTsvFile() throws Exception {

        File tsv = File.createTempFile("test_import_tsv", ".tsv");
        try {
            FileUtils.writeStringToFile(tsv, "id\tname\taddress\n" +
                            "123\tsomename\tanaddress\n" +
                            "124\tsomeothername\tanotherplace",
                    "UTF-8");

            WSResponse response = load(tsv.toURI().toString());

            assertThat(response.getStatus()).isEqualTo(OK);

            List<JsonNode> documents = PostgresqlStoreForTesting.findAll(REGISTER);

            assertThat(documents.size()).isEqualTo(2);

            assertThat(documents.get(0)).isNotNull();
            assertThat(documents.get(0).get("id").textValue()).isEqualTo("123");
            assertThat(documents.get(0).get("name").textValue()).isEqualTo("somename");
            assertThat(documents.get(0).get("address").textValue()).isEqualTo("anaddress");

            assertThat(documents.get(1)).isNotNull();
            assertThat(documents.get(1).get("id").textValue()).isEqualTo("124");
            assertThat(documents.get(1).get("name").textValue()).isEqualTo("someothername");
            assertThat(documents.get(1).get("address").textValue()).isEqualTo("anotherplace");
        } finally {
            tsv.delete();
        }

    }


    @Test
    public void testImportCsvFile() throws Exception {
        File csv = File.createTempFile("test_import_csv", ".csv");
        try {
            FileUtils.writeStringToFile(csv, "id,name,address\n" +
                            "123,somename,anaddress\n" +
                            "124,someothername,anotherplace",
                    "UTF-8");

            WSResponse response = load(csv.toURI().toString());

            assertThat(response.getStatus()).isEqualTo(OK);

            List<JsonNode> documents = PostgresqlStoreForTesting.findAll(REGISTER);

            assertThat(documents.size()).isEqualTo(2);

            assertThat(documents.get(0)).isNotNull();
            assertThat(documents.get(0).get("id").textValue()).isEqualTo("123");
            assertThat(documents.get(0).get("name").textValue()).isEqualTo("somename");
            assertThat(documents.get(0).get("address").textValue()).isEqualTo("anaddress");

            assertThat(documents.get(1)).isNotNull();
            assertThat(documents.get(1).get("id").textValue()).isEqualTo("124");
            assertThat(documents.get(1).get("name").textValue()).isEqualTo("someothername");
            assertThat(documents.get(1).get("address").textValue()).isEqualTo("anotherplace");
        } finally {
            csv.delete();
        }
    }

    @Test
    public void testBadRequestIfUrlIsNotSet() throws Exception {

        WSResponse response = WS.url(BASE_URL + "/load").get().get(TIMEOUT);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("{\"status\":400,\"message\":\"'url' parameter is not defined\"}");

    }
}
