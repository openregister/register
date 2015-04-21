package controllers.json;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.Test;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;

import java.io.File;
import java.util.ArrayList;

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

            ArrayList<Document> documents = Lists.newArrayList(collection().find());

            assertThat(documents.size()).isEqualTo(2);
            assertThat(documents.get(0).get("entry")).isNotNull();
            assertThat(documents.get(0).get("entry", Document.class).get("id")).isEqualTo("123");
            assertThat(documents.get(0).get("entry", Document.class).get("name")).isEqualTo("somename");
            assertThat(documents.get(0).get("entry", Document.class).get("address")).isEqualTo("anaddress");
            assertThat(documents.get(1).get("entry")).isNotNull();
            assertThat(documents.get(1).get("entry", Document.class).get("id")).isEqualTo("124");
            assertThat(documents.get(1).get("entry", Document.class).get("name")).isEqualTo("someothername");
            assertThat(documents.get(1).get("entry", Document.class).get("address")).isEqualTo("anotherplace");
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

            ArrayList<Document> documents = Lists.newArrayList(collection().find());

            assertThat(documents.size()).isEqualTo(2);
            assertThat(documents.get(0).get("entry")).isNotNull();
            assertThat(documents.get(0).get("entry", Document.class).get("id")).isEqualTo("123");
            assertThat(documents.get(0).get("entry", Document.class).get("name")).isEqualTo("somename");
            assertThat(documents.get(0).get("entry", Document.class).get("address")).isEqualTo("anaddress");
            assertThat(documents.get(1).get("entry")).isNotNull();
            assertThat(documents.get(1).get("entry", Document.class).get("id")).isEqualTo("124");
            assertThat(documents.get(1).get("entry", Document.class).get("name")).isEqualTo("someothername");
            assertThat(documents.get(1).get("entry", Document.class).get("address")).isEqualTo("anotherplace");
        } finally {
            csv.delete();
        }
    }

    @Test
    public void testBadRequestIfUrlIsNotSet() throws Exception {

        WSResponse response = WS.url("http://localhost:" + PORT + "/load").get().get(TIMEOUT);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("{\"status\":400,\"message\":\"'url' parameter is not defined\"}");

    }
}
