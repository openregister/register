package uk.gov.openregister.store;

import uk.gov.openregister.domain.Record;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Store {

    void save(Record s);

    void deleteAll();

    Optional<Record> findByKV(String key, String value);

    List<String> findAllByKeyValue(String key, String value);

    Optional<Record> findByHash(String hash);

    List<Record> search(Map<String, String> map);

    List<Record> search(String query);

    long count();

    void update(String hash, Record record);
}
