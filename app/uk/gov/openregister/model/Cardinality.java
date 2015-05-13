package uk.gov.openregister.model;

public enum Cardinality {

    ONE("1"), MANY("n");

    private String value;

    Cardinality(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Cardinality fromValue(String v) {
        if (MANY.getValue().equals(v)) return MANY;
        else return ONE;
    }
}
