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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecordVersionInfo that = (RecordVersionInfo) o;

        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;
        return !(timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null);

    }

    @Override
    public int hashCode() {
        int result = hash != null ? hash.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RecordVersionInfo{" +
                "hash='" + hash + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
