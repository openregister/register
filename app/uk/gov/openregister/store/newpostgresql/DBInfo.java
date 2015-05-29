package uk.gov.openregister.store.newpostgresql;

import uk.gov.openregister.model.Datatype;

import java.util.List;

import static uk.gov.openregister.model.Datatype.STRING;

public class DBInfo {
    public final String recordTableName;
    public final String versionTableName;
    public final String primaryKey;
    public final List<String> keys;

    public DBInfo(String registerName, String primaryKey, List<String> keys) {
        String tableNamePrefix = registerName.replaceAll("-", "_");
        this.recordTableName = tableNamePrefix + "_record";
        this.versionTableName = tableNamePrefix + "_version";
        this.primaryKey = primaryKey;
        this.keys = keys;
    }
}
