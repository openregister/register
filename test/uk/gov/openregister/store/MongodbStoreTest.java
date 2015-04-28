package uk.gov.openregister.store;

import controllers.conf.Register;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.conf.TestConfigurations;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.mongodb.MongodbStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MongodbStoreTest {

    public static final String COLLECTION = "store_tests";
    private Store store;

    @Before
    public void setUp() throws Exception {
        TestSettings.collection(COLLECTION).drop();
        Register schema = mock(Register.class);
        when(schema.keys()).thenReturn(Arrays.asList("aKey", "anotherKey"));
        store = new MongodbStore(TestConfigurations.MONGO_URI, COLLECTION, schema);
    }

    @Test
    public void testCreateRecord() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";

        store.save(new Record(Json.parse(json)));

        Document document = TestSettings.collection(COLLECTION).find().first();
        Document entry = document.get("entry", Document.class);
        assertThat(entry).isNotNull();
        assertThat(entry.get("key1")).isEqualTo("value1");
        assertThat(entry.get("key2")).isEqualTo("value2");
    }


    @Test
    public void testOnCreationAnHashIsCreated() {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        store.save(new Record(Json.parse(json)));

        Document document = TestSettings.collection(COLLECTION).find().first();
        assertThat(document.get("hash")).isEqualTo(expected);
    }


    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(Json.parse(json)));

        assertThat(store.findByKV("aKey", "AValue").get().toString()).isEqualTo(expected);
        assertThat(store.findByKV("anotherKey", "AnotherValue").get().toString()).isEqualTo(expected);

        assertThat(store.findByKV("anotherKey", "A")).isEqualTo(Optional.empty());
    }


    @Test
    public void testFindByHash() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(Json.parse(json)));

        Optional<Record> record = store.findByHash("b90e76e02d99f33a1750e6c4d2623c30511fde25");
        assertThat(record.get().toString()).isEqualTo(expected);
    }


    @Test
    public void testSearch() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

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

        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testEmptyRecordWhenNoEntryInDB(){

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(0);
    }

    @Test
    public void testCount() {
        String json1 = "{\"aKey\":\"aValue1\",\"anotherKey\":\"anotherValue1\"}";
        String json2 = "{\"aKey\":\"aValue2\",\"anotherKey\":\"anotherValue2\"}";

        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        assertThat(store.count()).isEqualTo(2);
    }
}
