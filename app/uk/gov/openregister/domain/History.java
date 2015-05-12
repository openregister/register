package uk.gov.openregister.domain;

import org.joda.time.DateTime;

public class History implements Comparable<History> {
    public final String hash;
    public final DateTime timestamp;

    public History(String hash, DateTime timestamp) {
        this.hash = hash;
        this.timestamp = timestamp;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(History obj) {
        return obj.timestamp.compareTo(this.timestamp);
    }
}
