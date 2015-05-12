package controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Optional;

public interface Representation {
    Result toResponse(int status, String message);

    Result toListOfRecords(List<Record> records) throws JsonProcessingException;

    Result toRecord(Optional<Record> recordO, List<RecordVersionInfo> history);
}
