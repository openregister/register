package uk.gov.openregister.store;

import com.google.common.collect.ImmutableSet;
import helper.DataRow;
import helper.PostgresqlStoreForTesting;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.postgresql.DBInfo;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

public class PostgresqlStoreTest {

    public static final String TABLE_NAME = "store_tests";
    public static final String HISTORY_TABLE_NAME = "store_tests_history";
    private PostgresqlStore store;

    @Before
    public void setUp() throws Exception {
        PostgresqlStoreForTesting.dropTables(TABLE_NAME);
        store = new PostgresqlStore(
                new DBInfo(TABLE_NAME, TABLE_NAME.toLowerCase(), Arrays.asList("store_tests", "anotherKey")),
                PostgresqlStoreForTesting.dataSource
        );
    }

    @Test
    public void save_insertsARecordWithMetadataAndHashAndInsertsIntoTheHistory() throws JSONException {
        String json = "{\"store_tests\": \"va'lue1\",\"key2\": \"value2\"}";
        String expectedhash = "13b7e046212329ca01bd66cc505028dd3ce993b3";
        store.save(new Record(json));

        List<DataRow> rows = PostgresqlStoreForTesting.findAll(TABLE_NAME);

        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).hash).isEqualTo(expectedhash);
        JSONAssert.assertEquals(json, rows.get(0).entry.toString(), true);
        assertNotNull(rows.get(0).metadata.creationTime);
        assertEquals("", rows.get(0).metadata.previousEntryHash);

        List<DataRow> historyRows = PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME);

        assertThat(historyRows.size()).isEqualTo(1);
        assertThat(historyRows.get(0).hash).isEqualTo(expectedhash);
        JSONAssert.assertEquals(json, historyRows.get(0).entry.toString(), true);
        assertNotNull(historyRows.get(0).metadata.creationTime);
        assertEquals("", historyRows.get(0).metadata.previousEntryHash);
    }

    @Test
    public void save_throwsExceptionWhenTryToInsertARecordWithDuplicateValueOfPrimaryKey() {
        String json = "{\"store_tests\": \"value1\",\"key2\": \"value2\"}";

        store.save(new Record(json));

        try {
            store.save(new Record(json.replaceAll("value2", "newValue")));
            fail("must throw exception");
        } catch (DatabaseException e) {
            assertEquals(1, PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME).size());
        }
    }

    @Test
    public void update_insertNewEntryOnlyWhenAnEntryWithSamePrimaryKeyAvailableAndInsertsNewHistoryEntry() throws JSONException {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.save(oldRecord);

        store.update(oldRecord.getHash(), newRecord);

        List<DataRow> rows = PostgresqlStoreForTesting.findAll(TABLE_NAME);
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).hash).isEqualTo(newRecord.getHash());
        JSONAssert.assertEquals(rows.get(0).entry.toString(), newRecord.getEntry().toString(), true);

        List<DataRow> historyRows = PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME);
        assertThat(historyRows.size()).isEqualTo(2);

        assertEquals(ImmutableSet.of(oldRecord.getHash(), newRecord.getHash()), historyRows.stream().map(row -> row.hash).collect(Collectors.toSet()));

    }

    @Test
    public void update_previousEntryHash_valueIsHashValueOfOldRecord() {
        String json1 = "{\"store_tests\":\"aValue\",\"key2\":\"anotherValue\"}";
        Record oldRecord = new Record(json1);
        store.save(oldRecord);

        Record newRecord = new Record(json1.replaceAll("anotherValue", "newValue"));
        store.update(oldRecord.getHash(), newRecord);

        String actualPreviousEntryHash = PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME).stream().filter(row -> row.hash.equals(newRecord.getHash())).map(row -> row.metadata.previousEntryHash).findFirst().get();
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
        } catch (DatabaseException e) {
            assertEquals(0, PostgresqlStoreForTesting.findAll(TABLE_NAME).size());
            assertEquals(0, PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME).size());
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
        } catch (DatabaseException e) {
            assertEquals(oldRecord.getHash(), PostgresqlStoreForTesting.findAll(TABLE_NAME).get(0).hash);
            assertEquals(1, PostgresqlStoreForTesting.findAll(HISTORY_TABLE_NAME).size());
        }
    }

    @Test
    public void testFindByKV() throws JSONException {
        String json = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"0b5f9e93b101ba410da10279229b6e0aa1411b85\",\"entry\":{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        Optional<Record> record = store.findByKV("store_tests", "aValue");
        JSONAssert.assertEquals(expected, record.get().toString(), true);
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
    public void findByHash_returnsRecordFromHistoryIfItDoesNotFindInPrimaryTable() {
        String json = "{\"store_tests\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        Record record1 = new Record(json);

        store.save(record1);

        Record record2 = new Record(json.replaceAll("anotherValue", "newValue"));
        store.update(record1.getHash(), record2);

        assertEquals(record2.getHash(), store.findByHash(record2.getHash()).get().getHash());
        assertEquals(record1.getHash(), store.findByHash(record1.getHash()).get().getHash());

    }

    @Test
    public void history_returnsAllHistoryForTheGivenKeyValueOrderedByLatest() {
        String json = "{\"store_tests\":\"aValue\",\"key\":\"value1\"}";
        Record record1 = new Record(json);
        store.save(record1);
        Record record2 = new Record(json.replace("value1", "value2"));
        store.update(record1.getHash(), record2);
        Record record3 = new Record(json.replace("value1", "value3"));
        store.update(record2.getHash(), record3);

        List<RecordVersionInfo> resultValues = store.history("store_tests", "aValue");

        assertThat(resultValues.size()).isEqualTo(3);

        assertThat(resultValues.get(0).hash).isEqualTo(record3.getHash());
        assertThat(resultValues.get(1).hash).isEqualTo(record2.getHash());
        assertThat(resultValues.get(2).hash).isEqualTo(record1.getHash());
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
        JSONAssert.assertEquals(expected, records.get(0).toString(), true);
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
        JSONAssert.assertEquals(expected, records.get(0).toString(), true);
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
