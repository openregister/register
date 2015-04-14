package uk.gov.openregister.store;

import uk.gov.openregister.domain.RegisterRow;

public abstract class Store {

    public Store(String databaseURI, String collection) {
        this.databaseURI = databaseURI;
        this.collection = collection;
    }

    protected String databaseURI;
    protected String collection;

    public abstract void save(String s);

    public abstract RegisterRow findByKV(String key, String value);

    public void create(RegisterRow row) {
        save(row.toString());
    }
}
