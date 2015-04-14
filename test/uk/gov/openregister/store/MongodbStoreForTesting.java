package uk.gov.openregister.store;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import uk.gov.openregister.conf.TestConfigurations;

import java.util.HashMap;
import java.util.Map;

public class MongodbStoreForTesting {

    public static MongoClientURI conf = new MongoClientURI(TestConfigurations.MONGO_URI);

    public static Map<String, String> settings(String cn) {
        HashMap<String, String> map = new HashMap<>();
        map.put("store.uri", conf.toString());
        map.put("register.name", cn);
        return map;
    }

    public static MongoCollection<Document> collection(String cn) {
        return new MongoClient(conf).getDatabase(conf.getDatabase()).getCollection(cn);
    }
}
