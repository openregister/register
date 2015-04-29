package uk.gov.openregister.validation;

public class ValidationResult {
    private boolean valid;
    private String[] messages;

    public ValidationResult(boolean valid, String[] messages) {
        this.valid = valid;
        this.messages = messages;
    }


    public boolean isValid() {
        return valid;
    }

    public String[] getMessages() {
        return messages;
    }
}
