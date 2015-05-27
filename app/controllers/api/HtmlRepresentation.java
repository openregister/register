package controllers.api;

import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.validation.ValidationError;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class HtmlRepresentation implements Representation {
    public Result toResponse(int status, String message, String registerName) {
        return status(status, views.html.error.render(registerName, message));
    }

    public Result toResponseWithErrors(int statusCode, List<ValidationError> errors, String registerName) {
        String validationErrorsString = errors.stream()
                .map(error -> String.format("%s: %s", error.key, error.message))
                .collect(Collectors.joining("; "));
        String errorMessage = String.format("Some fields had validation errors: %s", validationErrorsString);
        return toResponse(statusCode, errorMessage, registerName);
    }

    @Override
    public Result toListOfRecords(List<Record> records, Map<String, String> representationsMap, Register register) {
        return ok(views.html.entries.render(register, register.fields(), records, representationsMap));
    }

    @Override
    public Result toRecord(Record dbRecord, List<RecordVersionInfo> history, Map<String, String> representationsMap, Register register) {
        return ok(views.html.entry.render(register, register.fields(), dbRecord, history, representationsMap));
    }

    public static HtmlRepresentation instance = new HtmlRepresentation();
}
