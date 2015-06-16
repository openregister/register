package uk.gov.openregister.store.postgresql;

public class DatabaseSchema {
    private final Database database;
    private final DBInfo dbInfo;

    public DatabaseSchema(Database database, DBInfo dbInfo) {
        this.database = database;
        this.dbInfo = dbInfo;
    }

    public void drop() {
        //note:do not drop indexes because it takes long time to create indexes on big database
        database.execute("DROP TABLE IF EXISTS " + dbInfo.tableName);
        database.execute("DROP TABLE IF EXISTS " + dbInfo.tableName + "_row_count");
        database.execute("DROP TABLE IF EXISTS " + dbInfo.historyTableName);
        database.execute("DROP TRIGGER IF EXISTS " + dbInfo.tableName + "_row_count_trigger ON " + dbInfo.tableName + " CASCADE");
        database.execute("DROP FUNCTION IF EXISTS " + dbInfo.tableName + "_row_count_fn() CASCADE");
    }

    public void createIfNotExist() {
        database.execute("CREATE TABLE IF NOT EXISTS " + dbInfo.tableName + " (hash varchar(40) primary key,entry jsonb, lastUpdated timestamp without time zone, previousEntryHash varchar(40), searchable tsvector)");
        database.execute("CREATE TABLE IF NOT EXISTS " + dbInfo.historyTableName + " (hash varchar(40) primary key,entry jsonb, lastUpdated timestamp without time zone, previousEntryHash varchar(40))");
        database.execute("CREATE TABLE IF NOT EXISTS " + dbInfo.tableName + "_row_count (count integer)");

        database.select("SELECT count(*) from " + dbInfo.tableName + "_row_count").andThen(rs -> {
            rs.next();
            if (rs.getInt(1) != 1) {
                database.execute("INSERT INTO " + dbInfo.tableName + "_row_count values(0)");
            }
            return true;
        });

        database.execute(("CREATE OR REPLACE FUNCTION ~~~TABLE_NAME~~~_row_count_fn()  RETURNS trigger\n" +
                "AS $$\n" +
                "BEGIN\n" +
                "  IF TG_OP = 'INSERT' THEN\n" +
                "     EXECUTE 'UPDATE ~~~TABLE_NAME~~~_row_count set count=count + 1';\n" +
                "     RETURN NEW;\n" +
                "  ELSIF TG_OP = 'DELETE' THEN\n" +
                "     EXECUTE 'UPDATE ~~~TABLE_NAME~~~_row_count set count=count - 1';\n" +
                "     RETURN OLD;\n" +
                "  END IF;\n" +
                "  RETURN NULL;\n" +
                "  END;\n" +
                "$$ LANGUAGE plpgsql").replaceAll("~~~TABLE_NAME~~~", dbInfo.tableName));


        database.execute("DROP TRIGGER IF EXISTS " + dbInfo.tableName + "_row_count_trigger ON " + dbInfo.tableName + " CASCADE");
        database.execute("CREATE TRIGGER " + dbInfo.tableName + "_row_count_trigger \n" +
                " AFTER INSERT OR DELETE on " + dbInfo.tableName +
                " FOR EACH ROW EXECUTE PROCEDURE " + dbInfo.tableName + "_row_count_fn()");

        if (database.select("SELECT to_regclass('public." + dbInfo.tableName + "_lastUpdated_idx')")
                .andThen(r -> {
                    r.next();
                    return r.getString(1);
                }) == null) {


            database.execute("CREATE INDEX " + dbInfo.tableName + "_entry_idx ON " + dbInfo.tableName + " USING gin (entry)");
            database.execute("CREATE INDEX " + dbInfo.tableName + "_searchable_idx ON " + dbInfo.tableName + " USING gin(searchable)");

            database.execute("CREATE INDEX " + dbInfo.tableName + "_lastUpdated_idx ON " + dbInfo.tableName + " (lastUpdated DESC)");
            database.execute("CLUSTER " + dbInfo.tableName + " using " + dbInfo.tableName + "_lastUpdated_idx");

            database.execute("CREATE INDEX " + dbInfo.historyTableName + "_lastUpdated_idx ON " + dbInfo.historyTableName + " (lastUpdated DESC)");
            database.execute("CLUSTER " + dbInfo.historyTableName + " using " + dbInfo.historyTableName + "_lastUpdated_idx");
        }
    }
}
