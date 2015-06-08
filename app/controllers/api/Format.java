package controllers.api;

import uk.gov.openregister.config.Register;

public enum Format {
        json {
            @Override
            Representation createRepresentation(Register register) {
                return new JsonRepresentation(register);
            }
        },
        ttl{
            @Override
            Representation createRepresentation(Register register) {
                return new TurtleRepresentation(register);
            }
        },
        yaml{
            @Override
            Representation createRepresentation(Register register) {
                return new YamlRepresentation();
            }
        },
        html{
            @Override
            Representation createRepresentation(Register register) {
                return new HtmlRepresentation(register);
            }
        },
        tsv{
            @Override
            Representation createRepresentation(Register register) {
                return CSVRepresentation.tsvInstance(register);
            }
        },
        csv{
            @Override
            Representation createRepresentation(Register register) {
                return CSVRepresentation.csvInstance(register);
            }
        },
        atom{
            @Override
            Representation createRepresentation(Register register) {
                return new AtomRepresentation(register);
            }
        };


    abstract Representation createRepresentation(Register register);

    public static Representation representationFor(Register register, String format){
        try{
            return Format.valueOf(format).createRepresentation(register);
        }catch(Exception e){
            throw new RuntimeException("Format '" + format + "' not supported");
        }
    }
}

