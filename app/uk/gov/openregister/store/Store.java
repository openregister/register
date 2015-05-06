package uk.gov.openregister.store;

import uk.gov.openregister.domain.Record;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Store {

    public Store(String databaseURI) {
        this.databaseURI = databaseURI;
    }

    protected String databaseURI;

    public abstract void save(Record s);

    public abstract void deleteAll();

    public abstract Optional<Record> findByKV(String key, String value);

    public abstract Optional<Record> findByHash(String hash);

    public abstract List<Record> search(Map<String, String> map);

    public abstract List<Record> search(String query);

    public abstract long count();

    public abstract void update(String hash, String registerPrimaryKey, Record record);
}
