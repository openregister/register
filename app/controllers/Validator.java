package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.openregister.domain.Record;

import java.util.*;
import java.util.stream.Collectors;

public class Validator {

    List<String> keys;

    public Validator(List<String> keys) {
        this.keys = keys;
    }

    public ValidationResult validate(Record record) {

        Optional<String> invalidKeys = checkForInvalidKeys(record);
        Optional<String> missingKeys = checkForMissingKeys(record);

        String[] errors = Arrays.asList(invalidKeys, missingKeys).stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(String[]::new);


        if (errors.length > 0) return new ValidationResult(false, errors);
        else return new ValidationResult(true, new String[]{"Valid Record"});
    }

    private Optional<String> checkForInvalidKeys(Record record) {
        ArrayList<String> invalidKeys = new ArrayList<>();

        record.getEntry().fieldNames().forEachRemaining(key -> {
            if (!keys.contains(key)) invalidKeys.add(key);
        });

        if(!invalidKeys.isEmpty()) {

            String error = "The following keys are allowed in the record: ";
            String keys = invalidKeys.stream().collect(Collectors.joining(", "));

            return Optional.of(error + keys);
        } else {
            return Optional.empty();
        }
    }


    private Optional<String> checkForMissingKeys(Record record) {

        JsonNode entry = record.getEntry();
        String message = this.keys.stream()
                .filter(k -> !entry.has(k) || (!entry.get(k).isArray() && entry.get(k).asText().isEmpty()))
                .collect(Collectors.joining(", "));

        if(!message.isEmpty()) {
            return Optional.of("The following keys are mandatory but not found in record: " + message);
        } else {
            return Optional.empty();
        }
    }

}


class ValidationResult {
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