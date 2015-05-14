package uk.gov.openregister.config;

import play.db.DB;
import uk.gov.openregister.model.Field;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.DBInfo;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.List;
import java.util.stream.Collectors;


public abstract class Register {

    private Store store;

    public abstract InitResult init();

    public final Store store() {
        if(store ==null){
            store = new PostgresqlStore(new DBInfo(name(), name().toLowerCase(), fieldNames()), DB.getDataSource());
        }
        return store;
    }

    public abstract String friendlyName();

    public abstract String name();

    public abstract List<Field> fields();

    public List<String> fieldNames() {
        return fields().stream().map(Field::getName).collect(Collectors.toList());
    }

}
