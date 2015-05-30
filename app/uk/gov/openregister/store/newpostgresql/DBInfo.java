package uk.gov.openregister.store.newpostgresql;

public class DBInfo {
    public final String recordTableName;
    public final String versionTableName;
    public final String primaryKey;

    public DBInfo(String registerName, String primaryKey) {
        String tableNamePrefix = registerName.replaceAll("-", "_");
        this.recordTableName = tableNamePrefix + "_record";
        this.versionTableName = tableNamePrefix + "_version";
        this.primaryKey = primaryKey;
    }
}
