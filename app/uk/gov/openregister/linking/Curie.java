package uk.gov.openregister.linking;

import java.util.Optional;

public class Curie {
    public final String namespace;
    public final String identifier;

    public Curie(String namespace, String identifier) {
        this.namespace = namespace;
        this.identifier = identifier;
    }

    public static Optional<Curie> of(String raw) {
        try {
            String[] split = raw.split(":", 2);
            String namespace = split[0];
            String identifier = split[1];
            return Optional.of(new Curie(namespace, identifier));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
