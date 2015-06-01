package controllers.api;

import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;

public interface Representation {
    Result toListOfRecords(Register register,
                           List<Record> records,
                           Map<String, String[]> requestParams,
                           Map<String, String> representationsMap,
                           String previousPageLink,
                           String nextPageLink
    );

    Result toRecord(Register register,
                    Record record,
                    Map<String, String[]> requestParams,
                    Map<String, String> representationsMap,
                    List<RecordVersionInfo> history
    );

    boolean isPaginated();
}
