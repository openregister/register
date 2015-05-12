package uk.gov.openregister.config;

import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RegisterRegister extends Register {

    public static final List<Field> FIELDS = Arrays.asList(new Field("register"), new Field("name"),
            new Field("fields", "Fields", Datatype.of("list"), Cardinality.MANY, Optional.of("field")), new Field("text"));

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
