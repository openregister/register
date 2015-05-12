package controllers.api;

public class Representations {
    public static Representation representationFor(String representation) {
        if ("json".equalsIgnoreCase(representation)) {
            return JsonRepresentation.instance;
        } else if ("yaml".equalsIgnoreCase(representation)) {
            return YamlRepresentation.instance;
        } else {
            return HtmlRepresentation.instance;
        }
    }
}

