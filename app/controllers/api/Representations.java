package controllers.api;

public class Representations {
    public enum Format {
        json(JsonRepresentation.instance),
        ttl(TurtleRepresentation.instance),
        yaml(YamlRepresentation.instance),
        html(HtmlRepresentation.instance),
        tsv(CSVRepresentation.tsvInstance),
        csv(CSVRepresentation.csvInstance);

        public final Representation representation;

        Format(Representation representation) {
            this.representation = representation;
        }
    }

    public static Representation representationFor(String representation){
        try{
            return Format.valueOf(representation).representation;
        }catch(Exception e){
            throw new RuntimeException("Format '" + representation + "' not supported");
        }
    }
}

