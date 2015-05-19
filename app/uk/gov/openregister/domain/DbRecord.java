package uk.gov.openregister.domain;

public class DbRecord {
    private final Record record;
    private final Metadata metaData;

    public DbRecord(Record theRecord, Metadata theMetaData) {
        record = theRecord;
        metaData = theMetaData;
    }

    public Record getRecord() {
        return record;
    }

    public Metadata getMetaData() {
        return metaData;
    }
}
