package uk.gov.openregister.store.postgresql;

import java.util.List;

public class RegisterInfo {
    public final String tableName;
    public final String primaryKey;
    public final List<String> keys;

    public RegisterInfo(String tableName, String primaryKey, List<String> keys) {
        this.tableName = tableName.replaceAll("-", "_");
        this.primaryKey = primaryKey;
        this.keys = keys;
    }
}
