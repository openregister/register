package controllers.api.representation;

import uk.gov.openregister.config.Register;

public enum Format {
        json {
            @Override
            public Representation createRepresentation(Register register) {
                return new JsonRepresentation();
            }
        },
        ttl{
            @Override
            public Representation createRepresentation(Register register) {
                return new TurtleRepresentation(register);
            }
        },
        yaml{
            @Override
            public Representation createRepresentation(Register register) {
                return new YamlRepresentation();
            }
        },
        html{
            @Override
            public Representation createRepresentation(Register register) {
                return new HtmlRepresentation(register);
            }
        },
        tsv{
            @Override
            public Representation createRepresentation(Register register) {
                return CSVRepresentation.tsvInstance(register);
            }
        },
        csv{
            @Override
            public Representation createRepresentation(Register register) {
                return CSVRepresentation.csvInstance(register);
            }
        },
        atom{
            @Override
            public Representation createRepresentation(Register register) {
                return new AtomRepresentation(register);
            }
        };


    public abstract Representation createRepresentation(Register register);

    public static Representation representationFor(Register register, String format){
        try{
            return Format.valueOf(format).createRepresentation(register);
        }catch(Exception e){
            throw new RuntimeException("Format '" + format + "' not supported");
        }
    }
}

