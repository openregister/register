package uk.gov.openregister.store;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.Optional;

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
    public Optional<Record> findByKV(String key, String value) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("entry." + key, value);

        return findOne(whereQuery);
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("hash", hash);

        return findOne(whereQuery);
    }

    private Optional<Record> findOne(BasicDBObject whereQuery) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        FindIterable<Document> documents = collection.find(whereQuery);

        // FIXME what if I get more than one result?
        // FIXME should I validate the hash?
        Document first = documents.first();

        return Optional.ofNullable(first).map(document -> {
            Document node = first.get("entry", Document.class);
            return new Record(Json.parse(node.toJson()));
        });
    }
}
