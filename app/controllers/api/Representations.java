package controllers.api;

public class Representations {
    public enum Format {
        json(JsonRepresentation.instance),
        ttl(TurtleRepresentation.instance),
        yaml(YamlRepresentation.instance),
        html(HtmlRepresentation.instance);

        public final Representation representation;

        Format(Representation representation) {
            this.representation = representation;
        }
    }

    public static Representation representationFor(String representation) throws IllegalArgumentException {
        return getFormat(representation).representation;
    }

    private static Format getFormat(String representation) {
        try {
            return Format.valueOf(representation);
        } catch (NullPointerException e) {
            return Format.html;
        }
    }
}

