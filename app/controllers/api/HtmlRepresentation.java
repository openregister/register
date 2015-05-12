package controllers.api;

import controllers.conf.Register;
import play.mvc.Result;
import play.mvc.Results;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Optional;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class HtmlRepresentation implements Representation {
    @Override
    public Result toResponse(int status, String message) {
        return toHtmlResponse(status, message);
    }

    @Override
    public Result toListOfRecords(List<Record> records) {
        return ok(views.html.entries.render(Register.instance.fields(), records));
    }

    @Override
    public Result toRecord(Optional<Record> recordO, List<RecordVersionInfo> history) {
        return recordO.map(record ->
                ok(views.html.entry.render(Register.instance.fields(), record, history)))
                .orElse(toHtmlResponse(404, "Entry not found"));
    }

    private static Results.Status toHtmlResponse(int status, String message) {
        return status(status, views.html.error.render(message));
    }


    public static Representation instance = new HtmlRepresentation();
}
