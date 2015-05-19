package uk.gov.openregister.store;

import uk.gov.openregister.domain.DbRecord;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Store {

    void save(Record s);

    void deleteAll();

    Optional<DbRecord> findByKV(String key, String value);

    List<RecordVersionInfo> history(String key, String value);

    Optional<DbRecord> findByHash(String hash);

    List<DbRecord> search(Map<String, String> map);

    List<DbRecord> search(String query);

    long count();

    void update(String hash, Record record);
}
