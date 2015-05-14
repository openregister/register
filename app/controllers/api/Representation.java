package controllers.api;

import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;

public interface Representation {
    Result toListOfRecords(List<Record> records) throws Exception;

    Result toRecord(Record record, List<RecordVersionInfo> history);
}
