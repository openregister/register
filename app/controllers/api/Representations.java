package controllers.api;

public class Representations {
    public enum Format {
        Json("json"),
        Yaml("yaml");

        public final String identifier;
        private Format(final String theIdentifier){
            identifier = theIdentifier;
        }
    }

    public static Representation representationFor(String representation) {
        if (Format.Json.identifier.equalsIgnoreCase(representation)) {
            return JsonRepresentation.instance;
        } else if (Format.Yaml.identifier.equalsIgnoreCase(representation)) {
            return YamlRepresentation.instance;
        } else if ("turtle".equalsIgnoreCase(representation)) {
            return TurtleRepresentation.instance;
        } else {
            return HtmlRepresentation.instance;
        }
    }
}

