package controllers.conf;

import java.util.List;

public class RegisterInfo {
    public final String tableName;
    public final String primaryKey;
    public final List<String> keys;

    public RegisterInfo(String tableName, String primaryKey, List<String> keys) {
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.keys = keys;
    }
}
