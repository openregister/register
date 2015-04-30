package uk.gov.openregister.validation;

import java.util.List;

public class ValidationResult {

    List<String> invalidKeys;
    List<String> missingKeys;

    public ValidationResult(List<String> invalidKeys, List<String> missingKeys) {
        this.invalidKeys = invalidKeys;
        this.missingKeys = missingKeys;
    }

    public List<String> getInvalidKeys() {
        return invalidKeys;
    }

    public List<String> getMissingKeys() {
        return missingKeys;
    }

    public boolean isValid() {
        return invalidKeys.isEmpty() && missingKeys.isEmpty();
    }
}
