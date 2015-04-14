package uk.gov.openregister.store;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import play.libs.Json;
import uk.gov.openregister.domain.RegisterRow;

public class MongodbStore extends Store {


    private MongoClientURI conf = new MongoClientURI(databaseURI);
    private MongoDatabase db = new MongoClient(conf).getDatabase(conf.getDatabase());

    public MongodbStore(String databaseURI, String collection) {
        super(databaseURI, collection);
    }

    @Override
    public void save(String s) {

        MongoCollection<Document> collection = db.getCollection(this.collection);
        collection.insertOne(Document.parse(s));
    }

    @Override
    public RegisterRow findByKV(String key, String value) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("entry." + key, value);
        FindIterable<Document> documents = collection.find(whereQuery);

        // FIXME what if I get more than one result?
        // FIXME should I validate the hash?
        Document first = documents.first();
        if(first != null) {
            Document node = first.get("entry", Document.class);
            return new RegisterRow(Json.parse(node.toJson()));
        } else return null;
    }
}
