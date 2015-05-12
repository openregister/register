package uk.gov.openregister.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RegisterRegister extends Register {

    public static final List<Field> FIELDS = Arrays.asList(new Field("register"), new Field("name"),
            new Field("fields", "Fields", new Datatype("list"), Cardinality.MANY, Optional.of("field")), new Field("text"));

    @Override
    public String friendlyName() {
        return "Register";
    }

    @Override
    public String name() {
        return "register";
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
