package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.conf.TestConfigurations;
import uk.gov.openregister.domain.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class PostgresqlStoreTest {

    public static final String TABLE_NAME = "store_tests";

    @Before
    public void setUp() throws Exception {
        PostgresqlStoreForTesting.dropTable(TABLE_NAME);
    }

    @Test
    public void testCreateRecord() throws Exception {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json)));

        JsonNode entry = PostgresqlStoreForTesting.findFirstEntry(TABLE_NAME);
        assertThat(entry.get("key1").textValue()).isEqualTo("value1");
        assertThat(entry.get("key2").textValue()).isEqualTo("value2");
    }


    @Test
    public void testOnCreationAnHashIsCreated() throws Exception {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json)));

        String hash = PostgresqlStoreForTesting.findFirstHash(TABLE_NAME);
        assertThat(hash).isEqualTo(expected);
    }


    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json)));

        Optional<Record> record = store.findByKV("aKey", "aValue");
        assertThat(record.get().toString()).isEqualTo(expected);
    }

    @Test
    public void testFindByHash() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json)));

        Optional<Record> record = store.findByHash("b90e76e02d99f33a1750e6c4d2623c30511fde25");
        assertThat(record.get().toString()).isEqualTo(expected);
    }


    @Test
    public void testSearch() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

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

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "Val");
        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }


    @Test
    public void testSearchAll() {
        String json1 = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String json2 = "{\"aKey\":\"differentValue\",\"anotherKey\":\"anotherValue\"}";

        Store store = new PostgresqlStore(TestConfigurations.POSTGRESQL_URI, TABLE_NAME);
        store.save(new Record(Json.parse(json1)));
        store.save(new Record(Json.parse(json2)));

        HashMap<String, String> q = new HashMap<>();

        List<Record> records = store.search(q);
        assertThat(records.size()).isEqualTo(2);
    }
}
