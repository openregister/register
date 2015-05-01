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

            List<JsonNode> documents = PostgresqlStoreForTesting.findAllEntries(REGISTER);

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

            List<JsonNode> documents = PostgresqlStoreForTesting.findAllEntries(REGISTER);

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
