package controllers.api;

import controllers.App;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;

import java.util.List;
import java.util.Map;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class HtmlRepresentation implements Representation {
    public Result toResponse(int status, String message) {
        return status(status, views.html.error.render(message));
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap) {
        return ok(views.html.entries.render(App.instance.register.fields(), records, representationsMap));
    }

    @Override
    public Result toRecord(Record record, List<RecordVersionInfo> history, Map<String, String> representationsMap) {
        return ok(views.html.entry.render(App.instance.register.fields(), record, history, representationsMap));
    }

    public static HtmlRepresentation instance = new HtmlRepresentation();
}
