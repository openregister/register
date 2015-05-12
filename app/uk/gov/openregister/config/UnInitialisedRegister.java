package uk.gov.openregister.config;

import org.apache.commons.lang3.text.WordUtils;

import java.util.Collections;
import java.util.List;

public class UnInitialisedRegister extends Register {

    private String name;

    public UnInitialisedRegister(String name) {
        this.name = name;
    }

    @Override
    public String friendlyName() {
        return WordUtils.capitalize(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Field> fields() {
        return Collections.singletonList(new Field(name));
    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
