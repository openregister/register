package uk.gov.openregister.store;

import uk.gov.openregister.domain.Record;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Store {

    public Store(String databaseURI, String collection) {
        this.databaseURI = databaseURI;
        this.collection = collection;
    }

    protected String databaseURI;
    protected String collection;

    public abstract void save(String s);

    public abstract Optional<Record> findByKV(String key, String value);

    public void create(Record row) {
        save(row.toString());
    }

    public abstract Optional<Record> findByHash(String hash);

    public abstract List<Record> search(Map<String, String> map);
}
