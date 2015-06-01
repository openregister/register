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
    public Result toListOfRecords(Register register,
                                  List<Record> records,
                                  Map<String, String[]> requestParams,
                                  Map<String, String> representationsMap,
                                  String previousPageLink,
                                  String nextPageLink) {
        return ok(views.html.entries.render(register, records, representationsMap, previousPageLink, nextPageLink));
    }

    @Override
    public Result toRecord(Register register,
                           Record dbRecord,
                           Map<String, String[]> requestParams,
                           Map<String, String> representationsMap,
                           List<RecordVersionInfo> history) {
        return ok(views.html.entry.render(register, dbRecord, history, representationsMap));
    }

    @Override
    public boolean isPaginated() {
        return true;
    }

    public static HtmlRepresentation instance = new HtmlRepresentation();
}
