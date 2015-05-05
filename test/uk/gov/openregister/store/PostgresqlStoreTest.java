package uk.gov.openregister.store;

import controllers.conf.Register;
import helper.DataRow;
import helper.PostgresqlStoreForTesting;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void save_insertsARecordWithMetadataAndHash() throws JSONException {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        String expectedhash = "bd9715d749969faef3434484deb8f33cbb7eab8f";
        store.save(new Record(json));

        List<DataRow> rows = PostgresqlStoreForTesting.findAll(TABLE_NAME);

        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).hash).isEqualTo(expectedhash);
        JSONAssert.assertEquals(json, rows.get(0).entry.toString(), true);
        assertNotNull(rows.get(0).metadata.creationtime);
        assertEquals("", rows.get(0).metadata.previousEntryHash);
    }

    @Test
    public void update_insertNewEntryOnlyWhenAnEntryWithSamePrimaryKeyAvailable() {
        //assuming key1 is primary key
        String json1 = "{\"key1\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.update(oldRecord.getHash(), "key1", newRecord);
        assertThat(store.count()).isEqualTo(0);

        store.save(oldRecord);

        store.update(oldRecord.getHash(), "key1", newRecord);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    public void update_previousEntryHash_valueIsHashValueOfOldRecord() {
        String json1 = "{\"key1\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);
        store.save(oldRecord);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.update(oldRecord.getHash(), "key1", newRecord);

        String actualPreviousEntryHash = PostgresqlStoreForTesting.findAll(TABLE_NAME).stream().filter(row -> row.hash.equals(newRecord.getHash())).map(row -> row.metadata.previousEntryHash).findFirst().get();
        assertEquals(oldRecord.getHash(), actualPreviousEntryHash);
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
