package uk.gov.openregister.validation;

public class ValidationError {
    public String key;
    public String message;

    public ValidationError(String key, String message) {
        this.key = key;
        this.message = message;
    }

}
