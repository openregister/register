package controllers.api;

import controllers.html.Pagination;
import play.mvc.Http;
import play.mvc.Result;
import uk.gov.openregister.config.Register;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.validation.ValidationError;

import java.util.List;
import java.util.stream.Collectors;

import static play.mvc.Results.ok;
import static play.mvc.Results.status;

public class HtmlRepresentation implements Representation {

    private final Register register;

    public HtmlRepresentation(Register register) {
        this.register = register;
    }

    public Result toResponse(int status, String message) {
        return status(status, views.html.error.render(register.friendlyName(), message));
    }

    public Result toResponseWithErrors(int statusCode, List<ValidationError> errors) {
        String validationErrorsString = errors.stream()
                .map(error -> String.format("%s: %s", error.key, error.message))
                .collect(Collectors.joining("; "));
        String errorMessage = String.format("Some fields had validation errors: %s", validationErrorsString);
        return toResponse(statusCode, errorMessage);
    }

    @Override
    public Result toListOfRecords(List<Record> records,
                                  Http.Request request,
                                  Pagination pagination) {
        return ok(views.html.entries.render(register, records, request, pagination));
    }

    @Override
    public Result toRecord(Record dbRecord,
                           Http.Request request,
                           List<RecordVersionInfo> history) {
        return ok(views.html.entry.render(register, dbRecord, history, request));
    }

    @Override
    public boolean isPaginated() {
        return true;
    }
}
