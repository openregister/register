package controllers.json;

import org.bson.Document;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class FindEntriesTest extends ApplicationTests {


    @Test
    public void testFindOneByKey() throws Exception {

        String json = "{\"hash\":\"759f1921d04fb297f825c3fede183516dbede0b3\",\"entry\":{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}}";
        collection().insertOne(Document.parse(json));

        WSResponse response = getByKV("key1", "valuex", "json");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(json);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        String json = "{\"hash\":\"759f1921d04fb297f825c3fede183516dbede0b3\",\"entry\":{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}}";
        collection().insertOne(Document.parse(json));

        WSResponse response = getByHash("759f1921d04fb297f825c3fede183516dbede0b3", "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(json);
    }


    @Test
    public void testSearch() throws Exception {
        Document r1 = Document.parse(new Record(Json.parse("{\"key\":\"value1\"}")).toString());
        Document r2 = Document.parse(new Record(Json.parse("{\"key\":\"value2\"}")).toString());
        Document r3 = Document.parse(new Record(Json.parse("{\"key\":\"value1\",\"another\":\"value\"}")).toString());

        collection().insertMany(Arrays.asList(r1, r2, r3));

        WSResponse response = search("key", "value1", "json");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody())
                .isEqualTo("[" +
                        "{\"hash\":\"91a2797ccc4e7f9ada53c10a2b66fb188366eb07\",\"entry\":{\"key\":\"value1\"}}," +
                        "{\"hash\":\"129de5416d0800c6783de9abe70428214df4fe02\",\"entry\":{\"key\":\"value1\",\"another\":\"value\"}}" +
                        "]");
    }
}
