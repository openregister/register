package controllers.api;

import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;

public interface Representation {
    Result toListOfRecords(List<Record> records, Map<String, String> representationsMap, String previousPageLink, String nextPageLink, Register register) throws Exception;

    Result toRecord(Record record, List<RecordVersionInfo> history, Map<String, String> representationsMap, Register register);

    boolean isPaginated();
}
