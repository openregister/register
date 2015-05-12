package uk.gov.openregister.config;

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
        if (ONE.getValue().equals(v)) return ONE;
        else return MANY;
    }
}
