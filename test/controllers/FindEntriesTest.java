package controllers;

import org.bson.Document;
import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class FindEntriesTest extends ApplicationTests {


    @Test
    public void testFindOneByKey() throws Exception {

        String json = "{\"hash\":\"759f1921d04fb297f825c3fede183516dbede0b3\",\"entry\":{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}}";
        collection().insertOne(Document.parse(json));

        WSResponse response = getByKV("key1", "valuex");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(json);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        String json = "{\"hash\":\"759f1921d04fb297f825c3fede183516dbede0b3\",\"entry\":{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}}";
        collection().insertOne(Document.parse(json));

        WSResponse response = getByHash("759f1921d04fb297f825c3fede183516dbede0b3");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(json);
    }
}
