package controllers.api;

import controllers.App;
import play.mvc.Result;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.validation.ValidationError;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class HtmlRepresentation implements Representation {
    public Result toResponse(int status, String message) {
        return status(status, views.html.error.render(message));
    }

    public Result toResponseWithErrors(int statusCode, List<ValidationError> errors) {
        String validationErrorsString = errors.stream()
                .map(error -> String.format("%s: %s", error.key, error.message))
                .collect(Collectors.joining("; "));
        String errorMessage = String.format("Some fields had validation errors: %s", validationErrorsString);
        return toResponse(statusCode, errorMessage);
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap) {
        return ok(views.html.entries.render(App.instance.register.fields(), records, representationsMap));
    }

    @Override
    public Result toRecord(Record dbRecord, List<RecordVersionInfo> history, Map<String, String> representationsMap) {
        return ok(views.html.entry.render(App.instance.register.fields(), dbRecord, history, representationsMap));
    }

    public static HtmlRepresentation instance = new HtmlRepresentation();
}
