package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import uk.gov.openregister.domain.Entry;

public class MongodbStore extends Store {


    private MongoClientURI conf = new MongoClientURI(databaseURI);
    private MongoDatabase db = new MongoClient(conf).getDatabase(conf.getDatabase());

    public MongodbStore(String databaseURI, String collection) {
        super(databaseURI, collection);
    }

    @Override
    public void save(ObjectNode s) {

        MongoCollection<Document> collection = db.getCollection(this.collection);
        collection.insertOne(Document.parse(s.toString()));
    }

    @Override
    public Entry findByHash(String hash) {
        return null;
    }
}
