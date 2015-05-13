package uk.gov.openregister.model;

import uk.gov.openregister.model.datatype.AString;

import java.util.Arrays;
import java.util.List;

public abstract class Datatype {

    public static final AString DEFAULT = new AString();

    private static final List<Datatype> knownTypes = Arrays.asList(DEFAULT);


    public abstract String getName();

    public static Datatype of(String s) {
        return knownTypes.stream().filter(dt -> dt.getName().equals(s)).findFirst().orElse(new Datatype() {
            @Override
            public String getName() {
                return s;
            }
        });
    }
}
