package controllers;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;
import uk.gov.openregister.MongodbStoreForTesting;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

public class CreateEntryTest extends ApplicationTests {

    private static final String test_register = "test_register";

    @Before
    public void setUp() throws Exception {
        MongodbStoreForTesting.drop();
    }

    @Test
    public void testCreateAnEntryReturns202() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        running(testServer(PORT, fakeApplication(MongodbStoreForTesting.settings(test_register))), () -> {
            WSResponse response = postJson("/create", json);
            assertThat(response.getStatus()).isEqualTo(ACCEPTED);
        });
    }

    @Test
    public void testCreateAnEntryWithMalformedRequestReturns400() {
        String json = "this is not json";
        running(testServer(PORT), () -> {
            WSResponse response = postJson("/create", json);
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
        });
    }


    @Test
    public void testCreateAnEntryStoresTheEntryToTheDatabase() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        running(testServer(PORT, fakeApplication(MongodbStoreForTesting.settings(test_register))), () -> {
            WSResponse response = postJson("/create", json);
            assertThat(response.getStatus()).isEqualTo(ACCEPTED);

            Document document = MongodbStoreForTesting.collection(test_register).find().first();

            assertThat(document.get("entry").toString())
                    .isEqualTo(json);
        });
    }



}
