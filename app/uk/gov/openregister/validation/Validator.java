package uk.gov.openregister.validation;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.domain.Record;

import java.util.*;
import java.util.stream.Collectors;

public class Validator {

    List<String> keys;

    public Validator(List<String> keys) {
        this.keys = keys;
    }

    public List<ValError> validate(Record record) {

        List<ValError> errors = new ArrayList<>();
        List<ValError> invalidKeyErrors = StreamUtils.asStream(record.getEntry().fieldNames())
                .filter(key -> !keys.contains(key))
                .map(key -> new ValError(key, "Key not required"))
                .collect(Collectors.toList());

        errors.addAll(invalidKeyErrors);

        JsonNode entry = record.getEntry();
        List<ValError> missingKeyErrors =keys.stream()
                .filter(k -> !entry.has(k) || (!entry.get(k).isArray() && entry.get(k).asText().isEmpty()))
                .map(key -> new ValError(key, "Missing required key"))
                .collect(Collectors.toList());

        errors.addAll(missingKeyErrors);

      return errors;
    }
}

