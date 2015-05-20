package uk.gov.openregister.domain;

public class DbRecord {
    private final Record record;
    private final Metadata metadata;

    public DbRecord(Record theRecord, Metadata theMetaData) {
        record = theRecord;
        metadata = theMetaData;
    }

    public Record getRecord() {
        return record;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
