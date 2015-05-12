package uk.gov.openregister.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Optional;

public class Field {

    public static final Datatype STRING_TYPE = new Datatype("string");
    String name;
    String frendlyName;
    Datatype datatype;
    Cardinality cardinality;

    public Field(JsonNode node) {

        name = Optional.of(node.get("field")).map(JsonNode::asText).get();
        frendlyName = Optional.ofNullable(node.get("name")).map(JsonNode::asText).orElse(name);
        datatype = Optional.ofNullable(node.get("datatype")).map(d -> new Datatype(d.asText())).orElse(STRING_TYPE);
        cardinality = Optional.ofNullable(node.get("cardinality")).map(c -> Cardinality.fromValue(c.textValue())).orElse(Cardinality.ONE);
    }

    public Field(String name) {
        this.name = name;
        this.frendlyName = WordUtils.capitalize(name);
        this.datatype = STRING_TYPE;
        this.cardinality = Cardinality.ONE;
    }

    public Field(String name, Cardinality cardinality) {
        this.name = name;
        this.frendlyName = WordUtils.capitalize(name);
        this.datatype = new Datatype("string");
        this.cardinality = cardinality;
    }

    public Field(String name, String frendlyName, Datatype datatype, Cardinality cardinality) {
        this.name = name;
        this.frendlyName = frendlyName;
        this.datatype = datatype;
        this.cardinality = cardinality;
    }

    public String getName() {
        return name;
    }

    public String getFrendlyName() {
        return frendlyName;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }
}
