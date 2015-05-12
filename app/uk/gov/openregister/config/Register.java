package uk.gov.openregister.config;

import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.PostgresqlStore;
import uk.gov.openregister.store.postgresql.RegisterInfo;

import java.util.List;
import java.util.stream.Collectors;


public abstract class Register {

    private Store store;

    public final Store store() {
        if(store ==null){
            String uri = ApplicationConf.getString("store.uri");
            store = new PostgresqlStore(uri, new RegisterInfo(name(), name().toLowerCase(), fieldNames()));
        }
        return store;
    }

    public abstract String friendlyName();

    public abstract String name();

    public abstract List<Field> fields();

    public List<String> fieldNames() {
        return fields().stream().map(Field::getName).collect(Collectors.toList());
    }

    public abstract boolean isStarted();

}
