package controllers.api;

import controllers.html.Pagination;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;

public interface Representation {
    Result toListOfRecords(List<Record> records,
                           Http.Request request,
                           Pagination pagination
    );

    Result toRecord(Record record,
                    Http.Request request,
                    List<RecordVersionInfo> history
    );

    boolean isPaginated();
}
