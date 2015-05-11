package uk.gov.openregister.store.postgresql;

import java.util.List;

public class DBInfo {
    public final String tableName;
    public final String historyTableName;
    public final String primaryKey;
    public final List<String> keys;

    public DBInfo(String tableName, String primaryKey, List<String> keys) {
        this.tableName = tableName.replaceAll("-", "_");
        this.historyTableName = this.tableName + "_history";
        this.primaryKey = primaryKey;
        this.keys = keys;
    }
}
