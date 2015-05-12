package uk.gov.openregister.domain;

import org.joda.time.DateTime;

public class RecordVersionInfo implements Comparable<RecordVersionInfo> {
    public final String hash;
    public final DateTime timestamp;

    public RecordVersionInfo(String hash, DateTime timestamp) {
        this.hash = hash;
        this.timestamp = timestamp;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(RecordVersionInfo obj) {
        return obj.timestamp.compareTo(this.timestamp);
    }
}
