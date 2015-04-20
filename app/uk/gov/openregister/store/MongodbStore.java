package uk.gov.openregister.store;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongodbStore extends Store {


    private static final int LIMIT = 100;
    private MongoClientURI conf = new MongoClientURI(databaseURI);
    private MongoDatabase db = new MongoClient(conf).getDatabase(conf.getDatabase());
    private String collection;

    public MongodbStore(String databaseURI, String collection) {
        super(databaseURI);
        this.collection = collection;
    }

    @Override
    public void save(Record s) {

        MongoCollection<Document> collection = db.getCollection(this.collection);
        collection.insertOne(Document.parse(s.toString()));
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

    @Override
    public List<Record> search(Map<String, String> map) {
        // TODO, very inefficient, replace with elastic search
        BasicDBObject q = new BasicDBObject();

        for (String key : map.keySet()) {
            q.put("entry." + key, new BasicDBObject("$regex", ".*" + map.get(key) + ".*").append("$options", "i"));
        }

        return find(q);
    }

    private Optional<Record> findOne(BasicDBObject whereQuery) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        FindIterable<Document> documents = collection.find(whereQuery).limit(1);

        // FIXME what if I get more than one result?
        // FIXME should I validate the hash?
        Document first = documents.first();

        return Optional.ofNullable(first).map(document -> {
            Document node = first.get("entry", Document.class);
            return new Record(Json.parse(node.toJson()));
        });
    }

    private List<Record> find(BasicDBObject whereQuery) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        FindIterable<Document> documents = collection.find(whereQuery).limit(LIMIT);

        MongoIterable<Record> records = documents.map(record -> {
            Document node = record.get("entry", Document.class);
            return new Record(Json.parse(node.toJson()));
        });

        return Lists.newArrayList(records);
    }
}
