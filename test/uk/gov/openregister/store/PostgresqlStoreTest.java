package uk.gov.openregister.store;

import uk.gov.openregister.store.postgresql.RegisterInfo;
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
import static org.junit.Assert.*;

public class PostgresqlStoreTest {

    public static final String TABLE_NAME = "store_tests";
    private PostgresqlStore store;

    @Before
    public void setUp() throws Exception {
        PostgresqlStoreForTesting.dropTable(TABLE_NAME);
        store = new PostgresqlStore(PostgresqlStoreForTesting.POSTGRESQL_URI, new RegisterInfo(TABLE_NAME, TABLE_NAME.toLowerCase(),Arrays.asList("store_tests", "anotherKey")));
    }

    @Test
    public void save_insertsARecordWithMetadataAndHash() throws JSONException {
        String json = "{\"store_tests\": \"va'lue1\",\"key2\": \"value2\"}";
        String expectedhash = "13b7e046212329ca01bd66cc505028dd3ce993b3";
        store.save(new Record(json));

        List<DataRow> rows = PostgresqlStoreForTesting.findAll(TABLE_NAME);

        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).hash).isEqualTo(expectedhash);
        JSONAssert.assertEquals(json, rows.get(0).entry.toString(), true);
        assertNotNull(rows.get(0).metadata.creationtime);
        assertEquals("", rows.get(0).metadata.previousEntryHash);
    }

    @Test
    public void save_throwsExceptionWhenTryToInsertARecordWithDupliocateValueOfPrimaryKey() {
        String json = "{\"store_tests\": \"value1\",\"key2\": \"value2\"}";

        store.save(new Record(json));

        try {
            store.save(new Record(json.replaceAll("value2", "newValue")));
            fail("must throw exception");
        } catch (DatabaseException e) {
            //success
        }
    }

    @Test
    public void update_insertNewEntryOnlyWhenAnEntryWithSamePrimaryKeyAvailable() {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.save(oldRecord);

        store.update(oldRecord.getHash(), newRecord);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    public void update_previousEntryHash_valueIsHashValueOfOldRecord() {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);
        store.save(oldRecord);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.update(oldRecord.getHash(), newRecord);

        String actualPreviousEntryHash = PostgresqlStoreForTesting.findAll(TABLE_NAME).stream().filter(row -> row.hash.equals(newRecord.getHash())).map(row -> row.metadata.previousEntryHash).findFirst().get();
        assertEquals(oldRecord.getHash(), actualPreviousEntryHash);
    }

    @Test
    public void update_throwsExceptionWhenThereIsNoRecord() {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));

        try {
            store.update(oldRecord.getHash(), newRecord);
            fail("Must fail");
        } catch (RuntimeException e) {
            //success
        }
    }

    @Test
    public void update_throwsExceptionWhenTryToUpdateThePrimaryKey() {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);
        store.save(oldRecord);

        Record newRecord = new Record(json1.replaceAll("aValue", "new'Value"));

        try {
            store.update(oldRecord.getHash(), newRecord);
            fail("Must fail");
        } catch (RuntimeException e) {
            //success
        }
    }

    @Test
    public void update_throwsExceptionWhenTryToUpdateOldRecord() {
        String json = "{\"store_tests\":\"aValue\",\"key2\":\"key2Value\"}";
        Record record1 = new Record(json);
        store.save(record1);

        Record record2 = new Record(json.replaceAll("key2Value", "newValue"));
        store.update(record1.getHash(), record2);

        store.update(record2.getHash(), new Record(json.replaceAll("key2Value", "newValue1")));

        Record record4 = new Record(json.replaceAll("key2Value", "newValue2"));
        try {
            store.update(record2.getHash(), record4);
            fail("must fail");
        } catch (RuntimeException e) {
            //success
        }
    }

    @Test
    public void testFindByKV() throws JSONException {
        String json = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        Optional<Record> record = store.findByKV("store_tests", "aValue");
        JSONAssert.assertEquals( expected, record.get().toString(),true);
    }

    @Test
    public void testFindByHash() throws JSONException {
        String json = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        Optional<Record> record = store.findByHash("0b5f9e93b101ba410da10279229b6e0aa1411b85");
        JSONAssert.assertEquals(expected, record.get().toString(), true);
    }


    @Test
    public void testSearch() throws JSONException {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("store_tests", "aV");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        JSONAssert.assertEquals( expected, records.get(0).toString(),true);
    }

    @Test
    public void testSearchEverywhere() throws JSONException {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("store_tests", "Val");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        JSONAssert.assertEquals(expected, records.get(0).toString(), true);
    }

    @Test
    public void testSearchCaseInsensitive() throws JSONException {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("store_tests", "aval");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        JSONAssert.assertEquals(expected,records.get(0).toString(), true);
    }

    @Test
    public void testSearchWithMultipleValues() throws JSONException {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"different\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        q.put("store_tests", "aval");
        q.put("anotherKey", "anotherValue");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        JSONAssert.assertEquals(expected, records.get(0).toString(), true);
    }

    @Test
    public void testSearchAll() {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        HashMap<String, String> q = new HashMap<>();

        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testSearchWithQuery() {
        String json1 = "{\"store_tests\":\"aValue1\",\"anotherKey\":\"anotherThing\"}";
        String json2 = "{\"store_tests\":\"different\",\"anotherKey\":\"aValue1\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(2);
    }

    @Test
    public void testCount() {
        String json1 = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"store_tests\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        assertThat(store.count()).isEqualTo(2);
    }
}
