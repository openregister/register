package uk.gov.openregister.config;

import uk.gov.openregister.model.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FieldRegister extends Register {

    public static final List<Field> FIELDS = Arrays.asList(new Field("field"), new Field("name"), new Field("datatype"), new Field("register", Optional.of("register")), new Field("cardinality"), new Field("text"));


    @Override
    public String friendlyName() {
        return "Field";
    }

    @Override
    public String name() {
        return "field";
    }

    @Override
    public List<Field> fields() {
        return FIELDS;
    }

    @Override
    public boolean isStarted() {
        return true;
    }
}
