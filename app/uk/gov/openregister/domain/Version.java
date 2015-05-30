package uk.gov.openregister.domain;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import uk.gov.openregister.JsonObjectMapper;
import uk.gov.openregister.crypto.Digest;
import uk.gov.openregister.store.DatabaseConflictException;
import uk.gov.openregister.store.DatabaseException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Version {
    private final DateTime creationTime;
    private final Map<String,String> records;
    private final String parentHash;

    public Version(DateTime creationTime, Map<String, String> records, String parentHash) {
        this.creationTime = creationTime;
        this.records = records;
        this.parentHash = parentHash;
    }

    public Version withNewRecord(String primaryKey, String hash) {
        if (records.containsKey(primaryKey)) {
            // XXX is this the right type?
            throw new DatabaseException("A record with key " + primaryKey + " already exists.");
        }
        ImmutableMap<String, String> newRecords = ImmutableMap.<String,String>builder().putAll(records)
                .put(primaryKey, hash)
                .build();
        return new Version(DateTime.now(), newRecords, getHash());
    }

    public Version withUpdatedRecord(String primaryKey, String oldHash, String newHash) {
        if (!Objects.equals(records.get(primaryKey), oldHash)) {
            throw new DatabaseConflictException("Either this record is outdated or attempted to update the primary key value.");
        }
        Map<String,String> newRecords = records.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getKey().equals(primaryKey) ? newHash : e.getValue()));
        return new Version(DateTime.now(), newRecords, getHash());
    }

    public String getHash() {
        // XXX could we memoize this?
        return Digest.shasum(creationTime.toString() + JsonObjectMapper.convertToString(records) + parentHash);
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public Map<String, String> getRecords() {
        return records;
    }

    public String getParent() {
        return parentHash;
    }
}
