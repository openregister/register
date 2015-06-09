package uk.gov.openregister.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Datatype {
    private static final List<Datatype> knownTypes = new ArrayList<>();


    private Datatype() {
        knownTypes.add(this);
    }

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

    public static final Datatype COLOUR = new Datatype() {
        @Override
        public String getName() {
            return "colour";
        }
    };

    public static final Datatype CURIE = new Datatype() {
        @Override
        public String getName() {
            return "curie";
        }
    };

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
