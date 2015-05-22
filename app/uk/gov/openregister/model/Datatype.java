package uk.gov.openregister.model;

import java.util.Arrays;
import java.util.List;

public abstract class Datatype {

    public static final Datatype STRING = new Datatype() {
        @Override
        public String getName() {
            return "string";
        }
    };

    public static final Datatype TEXT = new Datatype() {
        @Override
        public String getName() {
            return "text";
        }
    };

    private static final List<Datatype> knownTypes = Arrays.asList(STRING, TEXT);


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
