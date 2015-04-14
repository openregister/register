package uk.gov.openregister.store;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.conf.TestConfigurations;
import uk.gov.openregister.domain.RegisterRow;

import static org.fest.assertions.Assertions.assertThat;

public class StoreTest {

    public static final String COLLECTION = "store_tests";

    @Before
    public void setUp() throws Exception {
        MongodbStoreForTesting.collection(COLLECTION).drop();
    }

    @Test
    public void testCreateEntry() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        String expected = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.create(new RegisterRow(Json.parse(json)));

        Document document = MongodbStoreForTesting.collection(COLLECTION).find().first();
        Document entry = document.get("entry", Document.class);
        assertThat(entry).isNotNull();
        assertThat(entry.get("key1")).isEqualTo("value1");
        assertThat(entry.get("key2")).isEqualTo("value2");
    }


    @Test
    public void testOnCreationAnHashIsCreated() {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.create(new RegisterRow(Json.parse(json)));

        Document document = MongodbStoreForTesting.collection(COLLECTION).find().first();
        assertThat(document.get("hash")).isEqualTo(expected);
    }


    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.create(new RegisterRow(Json.parse(json)));

                RegisterRow entry = store.findByKV("aKey", "aValue");
                assertThat(entry.toString()).isEqualTo(expected);
            }


        }
