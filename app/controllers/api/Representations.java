package controllers.api;

public class Representations {
    public enum Format {
        json("json", JsonRepresentation.instance),
        turtle("turtle", TurtleRepresentation.instance),
        yaml("yaml", YamlRepresentation.instance),
        html("html", HtmlRepresentation.instance);

        public final String identifier;
        public final Representation representation;

        private Format(final String theIdentifier, Representation representation) {
            identifier = theIdentifier;
            this.representation = representation;
        }
    }

    public static Representation representationFor(String representation) {
        return getFormat(representation).representation;
    }

    private static Format getFormat(String representation) {
        try {
            return Format.valueOf(representation);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Format.html;
        }
    }
}

