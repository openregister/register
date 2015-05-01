package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.conf.Register;
import helper.PostgresqlStoreForTesting;
import org.junit.Before;
import org.junit.Test;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresqlStoreTest {

    public static final String TABLE_NAME = "store_tests";
    private PostgresqlStore store;

    @Before
    public void setUp() throws Exception {
        Register schema = mock(Register.class);
        when(schema.keys()).thenReturn(Arrays.asList("aKey", "anotherKey"));
        PostgresqlStoreForTesting.dropTable(TABLE_NAME);
        store = new PostgresqlStore(PostgresqlStoreForTesting.POSTGRESQL_URI, TABLE_NAME, schema.keys());
    }

    @Test
    public void testCreateRecord() throws Exception {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";

        store.save(new Record(json));

        JsonNode entry = PostgresqlStoreForTesting.findFirstEntry(TABLE_NAME);
        assertThat(entry.get("key1").textValue()).isEqualTo("value1");
        assertThat(entry.get("key2").textValue()).isEqualTo("value2");
    }

    @Test
    public void testOnCreationAnHashIsCreated() throws Exception {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        store.save(new Record(json));

        String hash = PostgresqlStoreForTesting.findFirstHash(TABLE_NAME);
        assertThat(hash).isEqualTo(expected);
    }

    @Test
    public void update_insertNewEntryOnlyWhenAnEntryWithSamePrimaryKeyAvailable() {
        //assuming key1 is primary key
        String json1 = "{\"key1\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.update(oldRecord.getHash(), "key1", newRecord);
        assertThat(store.count()).isEqualTo(0);

        store.save(new Record(json1));

        store.update(oldRecord.getHash(), "key1", newRecord);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        Optional<Record> record = store.findByKV("aKey", "aValue");
        assertThat(record.get().toString()).isEqualTo(expected);
    }

    @Test
    public void testFindByHash() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        Optional<Record> record = store.findByHash("b90e76e02d99f33a1750e6c4d2623c30511fde25");
        assertThat(record.get().toString()).isEqualTo(expected);
    }


    @Test
    public void testSearch() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "aV");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchEverywhere() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "Val");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchCaseInsensitive() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "aval");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchWithMultipleValues() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "aval");
        q.put("anotherKey", "anotherValue");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchAll() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testSearchWithQuery() {
        String json1 = "{\"aKey\":\"aValue1\",\"anotherKey\":\"anotherThing\"}";
        String json2 = "{\"aKey\":\"different\",\"anotherKey\":\"aValue1\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testCount() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        assertThat(store.count()).isEqualTo(2);
    }
}
