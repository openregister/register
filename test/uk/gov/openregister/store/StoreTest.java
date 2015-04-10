package uk.gov.openregister.store;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.MongodbStoreForTesting;
import uk.gov.openregister.conf.TestConfigurations;
import uk.gov.openregister.domain.Entry;

import static org.fest.assertions.Assertions.assertThat;

public class StoreTest {

    public static final String COLLECTION = "store_tests";

    @Before
    public void setUp() throws Exception {
        MongodbStoreForTesting.drop();
    }

    @Test
    public void testCreateEntry() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        String expected = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.create(new Entry(Json.parse(json)));

        Document document = MongodbStoreForTesting.collection(COLLECTION).find().first();
        assertThat(document.getString("entry")).isEqualTo(expected);
    }


    @Test
    public void testOnCreationAnHashIsCreated() {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.create(new Entry(Json.parse(json)));

        Document document = MongodbStoreForTesting.collection(COLLECTION).find().first();
        assertThat(document.get("hash")).isEqualTo(expected);
    }


}
