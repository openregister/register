package uk.gov.openregister.store.mongodb;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import scala.NotImplementedError;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.store.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MongodbStore extends Store {

    private static final int LIMIT = 100;
    private MongoClientURI conf = new MongoClientURI(databaseURI);
    private MongoDatabase db = new MongoClient(conf).getDatabase(conf.getDatabase());
    private String collection;
    private List<String> keys;

    public MongodbStore(String databaseURI, String collection, List<String> keys) {
        super(databaseURI);
        this.collection = collection;
        this.keys = keys;
    }

    @Override
    public void save(Record s) {

        MongoCollection<Document> collection = db.getCollection(this.collection);
        collection.insertOne(Document.parse(s.toString()));
    }

    @Override
    public Optional<Record> findByKV(String key, String value) {
        return findOne(new BasicDBObject("entry." + key, value));
    }

    @Override
    public Optional<Record> findByHash(String hash) {
        return findOne(new BasicDBObject("hash", hash));
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

    @Override
    public List<Record> search(String query) {

        BasicDBList q = keys.stream()
                .map(key -> new BasicDBObject("entry." + key, new BasicDBObject("$regex", ".*" + query + ".*").append("$options", "i")))
                .collect(Collectors.toCollection(BasicDBList::new));

        if (q.isEmpty()) {
            return new ArrayList<>();
        }

        return find(new BasicDBObject("$or", q));
    }

    @Override
    public long count() {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        return collection.count();
    }

    @Override
    public void update(String hash, String registerPrimaryKey, Record record) {
        throw new NotImplementedError();
    }

    private Optional<Record> findOne(BasicDBObject whereQuery) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        FindIterable<Document> documents = collection.find(whereQuery).limit(1);

        // FIXME what if I get more than one result?
        // FIXME should I validate the hash?
        Document first = documents.first();

        return Optional.ofNullable(first).map(document -> {
            Document node = first.get("entry", Document.class);
            return new Record(node.toJson());
        });
    }

    private List<Record> find(BasicDBObject whereQuery) {
        MongoCollection<Document> collection = db.getCollection(this.collection);
        FindIterable<Document> documents = collection.find(whereQuery).limit(LIMIT);

        MongoIterable<Record> records = documents.map(record -> {
            Document node = record.get("entry", Document.class);
            return new Record(node.toJson());
        });

        return Lists.newArrayList(records);
    }
}
