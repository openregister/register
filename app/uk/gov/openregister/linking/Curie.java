package uk.gov.openregister.linking;

public class Curie {
    public final String namespace;
    public final String identifier;

    public Curie(String namespace, String identifier) {
        this.namespace = namespace;
        this.identifier = identifier;
    }
}
