package uk.gov.openregister.config;

import org.apache.commons.lang3.text.WordUtils;
import uk.gov.openregister.FieldProvider;
import uk.gov.openregister.model.Field;

import java.util.Collections;
import java.util.List;

public class GenericRegister extends Register {

    public static final int TIMEOUT = 30000;
    private String name;
    private List<Field> fields;
    private String friendlyName;

    public GenericRegister(String name) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name);
        this.fields = Collections.singletonList(new Field(name));
    }

    @Override
    public InitResult init() {

        InitResult result = new InitResult(false);

        FieldProvider.getFields(name, (status, url) -> result.errors().add("Error: got status " + status + " calling " + url));
        if(result.errors.isEmpty()) result.started = true;
        return result;
    }

    @Override
    public String friendlyName() {
        return friendlyName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Field> fields() {
        return fields;
    }

}
