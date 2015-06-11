package uk.gov.openregister.store;

import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Store {

    void save(Record record);

    /**
     * Batch import without checking for duplicated primary keys. Used for the initial import
     * @param records
     */
    void fastImport(List<Record> records);

    void deleteAll();

    Optional<Record> findByKV(String key, String value);

    List<RecordVersionInfo> previousVersions(String hash);

    Optional<Record> findByHash(String hash);

    List<Record> search(Map<String, String> map, int offset, int limit, boolean historic, boolean exact);

    List<Record> search(String query, int offset, int limit, boolean historic);

    long count();

    void update(String hash, Record record);
}
