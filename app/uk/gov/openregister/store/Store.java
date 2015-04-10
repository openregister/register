package uk.gov.openregister.store;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Play;
import play.libs.Json;
import uk.gov.openregister.domain.Entry;

public abstract class Store {

    public Store(String databaseURI, String collection) {
        this.databaseURI = databaseURI;
        this.collection = collection;
    }

    protected String databaseURI;
    protected String collection;

    public abstract void save(ObjectNode s);
    public abstract Entry findByHash(String hash);

    public void create(Entry entry) {

        ObjectNode node = Json.newObject();

        node.put("hash", entry.getHash());
        node.put("entry", entry.getRaw());

        save(node);

    }
}
