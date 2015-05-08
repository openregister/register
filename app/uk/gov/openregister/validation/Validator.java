package uk.gov.openregister.validation;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.domain.Record;

import java.util.*;
import java.util.stream.Collectors;

public class Validator {


    private List<String> mandatoryKeys;
    private List<String> validKeys;

    public Validator(List<String>  mandatoryKeys, List<String> validKeys) {
        this.mandatoryKeys = mandatoryKeys;
        this.validKeys = validKeys;
    }

    public List<ValidationError> validate(Record record) {

        List<ValidationError> errors = new ArrayList<>();
        List<ValidationError> invalidKeyErrors = StreamUtils.asStream(record.getEntry().fieldNames())
                .filter(key -> !validKeys.contains(key))
                .map(key -> new ValidationError(key, "Key not required"))
                .collect(Collectors.toList());

        errors.addAll(invalidKeyErrors);

        JsonNode entry = record.getEntry();
        List<ValidationError> missingKeyErrors =mandatoryKeys.stream()
                .filter(k -> !entry.has(k) || (!entry.get(k).isArray() && entry.get(k).asText().isEmpty()))
                .map(key -> new ValidationError(key, "Missing required key"))
                .collect(Collectors.toList());

        errors.addAll(missingKeyErrors);

      return errors;
    }
}

