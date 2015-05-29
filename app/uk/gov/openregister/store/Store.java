package uk.gov.openregister.store;

import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Store {

    void save(Record s);

    void deleteAll();

    Optional<Record> findByKV(String key, String value);

    List<RecordVersionInfo> previousVersions(String hash);

    Optional<Record> findByHash(String hash);

    List<Record> search(Map<String, String> map, int offset, int limit, PostgresqlStore.SortBy Key);

    List<Record> search(String query, int offset, int limit, PostgresqlStore.SortBy sortBy);

    long count();

    void update(String hash, Record record);
}
