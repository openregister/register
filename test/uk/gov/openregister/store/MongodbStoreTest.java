package uk.gov.openregister.store;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.conf.TestConfigurations;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.mongodb.MongodbStore;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class MongodbStoreTest {

    public static final String COLLECTION = "store_tests";

    @Before
    public void setUp() throws Exception {
        MongodbStoreForTesting.collection(COLLECTION).drop();
    }

    @Test
    public void testCreateRecord() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

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
        store.save(new Record(Json.parse(json)));

        Document document = MongodbStoreForTesting.collection(COLLECTION).find().first();
        assertThat(document.get("hash")).isEqualTo(expected);
    }


    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

        Optional<Record> record = store.findByKV("aKey", "aValue");
        assertThat(record.get().toString()).isEqualTo(expected);
    }

    @Test
    public void testFindByHash() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

        Optional<Record> record = store.findByHash("b90e76e02d99f33a1750e6c4d2623c30511fde25");
        assertThat(record.get().toString()).isEqualTo(expected);
    }


    @Test
    public void testSearch() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "aV");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }



    @Test
    public void testSearchEverywhere() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "Val");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchCaseInsensitive() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json)));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "avalue");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchWithQuery() {
        String json1 = "{\"aKey\":\"aValue1\",\"anotherKey\":\"anotherThing\"}";
        String json2 = "{\"aKey\":\"different\",\"anotherKey\":\"aValue1\"}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testCount() {
        String json1 = "{\"aKey\":\"aValue1\",\"anotherKey\":\"anotherValue1\"}";
        String json2 = "{\"aKey\":\"aValue2\",\"anotherKey\":\"anotherValue2\"}";

        Store store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION);
        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        assertThat(store.count()).isEqualTo(2);
    }
}
