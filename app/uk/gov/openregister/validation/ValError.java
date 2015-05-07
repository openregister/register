package uk.gov.openregister.validation;

public class ValError {
    public String key;
    public String message;

    public ValError(String key, String message) {
        this.key = key;
        this.message = message;
    }

}
