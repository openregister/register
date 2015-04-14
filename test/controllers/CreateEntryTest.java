package controllers;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;
import uk.gov.openregister.store.MongodbStoreForTesting;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

public class CreateEntryTest extends ApplicationTests {

    @Test
    public void testCreateAnEntryReturns202() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);
    }

    @Test
    public void testCreateAnEntryWithMalformedRequestReturns400() {
        String json = "this is not json";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testCreateAnEntryStoresTheEntryToTheDatabase() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        Document document = collection().find().first();

        assertThat(document.get("entry")).isNotNull();
        assertThat(document.get("entry", Document.class).get("key1")).isEqualTo("value1");
        assertThat(document.get("entry", Document.class).get("key2")).isEqualTo("value2");
    }


}
