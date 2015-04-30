/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.openregister.store;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import controllers.conf.Register;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
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

    public static final String MONGO_URI = "mongodb://localhost/test-openregister";
    public static final String COLLECTION = "store_tests";
    private Store store;

    public static MongoClientURI conf = new MongoClientURI(MONGO_URI);
    public static MongoCollection<Document> collection(String cn) {
        return new MongoClient(conf).getDatabase(conf.getDatabase()).getCollection(cn);
    }

    @Before
    public void setUp() throws Exception {
        collection(COLLECTION).drop();
        Register schema = mock(Register.class);
        when(schema.keys()).thenReturn(Arrays.asList("aKey", "anotherKey"));
        store = new MongodbStore(MONGO_URI, COLLECTION, schema.keys());
    }

    @Test
    public void testCreateRecord() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";

        store.save(new Record(json));

        Document document = collection(COLLECTION).find().first();
        Document entry = document.get("entry", Document.class);
        assertThat(entry).isNotNull();
        assertThat(entry.get("key1")).isEqualTo("value1");
        assertThat(entry.get("key2")).isEqualTo("value2");
    }


    @Test
    public void testOnCreationAnHashIsCreated() {
        String json = "{\"foo\":\"Foo Value\"}";
        String expected = "257b86bf0b88dbf40cacff2b649f763d585df662";

        store.save(new Record(json));

        Document document = collection(COLLECTION).find().first();
        assertThat(document.get("hash")).isEqualTo(expected);
    }


    @Test
    public void testFindByKV() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        assertThat(store.findByKV("aKey", "aValue").get().toString()).isEqualTo(expected);
        assertThat(store.findByKV("anotherKey", "anotherValue").get().toString()).isEqualTo(expected);

        assertThat(store.findByKV("anotherKey", "AnotherValue")).isEqualTo(Optional.empty());
        assertThat(store.findByKV("anotherKey", "A")).isEqualTo(Optional.empty());
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
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "aV");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }



    @Test
    public void testSearchEverywhere() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "Val");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
    }

    @Test
    public void testSearchCaseInsensitive() {
        String json = "{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}";
        String expected = "{\"hash\":\"b90e76e02d99f33a1750e6c4d2623c30511fde25\",\"entry\":{\"aKey\":\"aValue\",\"anotherKey\":\"anotherValue\"}}";

        store.save(new Record(json));

        HashMap<String, String> q = new HashMap<>();

        q.put("aKey", "avalue");
        List<Record> records = store.search(q);
        assertThat(records.get(0).toString()).isEqualTo(expected);
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
    public void testEmptyRecordWhenNoEntryInDB(){

        List<Record> records = store.search("value");
        assertThat(records.size()).isEqualTo(0);
    }

    @Test
    public void testCount() {
        String json1 = "{\"aKey\":\"aValue1\",\"anotherKey\":\"anotherValue1\"}";
        String json2 = "{\"aKey\":\"aValue2\",\"anotherKey\":\"anotherValue2\"}";

        store.save(new Record(json1));
        store.save(new Record(json2));

        assertThat(store.count()).isEqualTo(2);
    }
}
