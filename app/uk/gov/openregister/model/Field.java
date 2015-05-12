package uk.gov.openregister.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Optional;

public class Field {

    String name;
    String frendlyName;
    Datatype datatype;
    Cardinality cardinality;
    Optional<String> register;

    public Field(JsonNode node) {

        name = Optional.of(node.get("field")).map(JsonNode::asText).get();
        frendlyName = Optional.ofNullable(node.get("name")).map(JsonNode::asText).orElse(WordUtils.capitalize(name));
        datatype = Optional.ofNullable(node.get("datatype")).map(d -> Datatype.of(d.asText())).orElse(Datatype.DEFAULT);
        cardinality = Optional.ofNullable(node.get("cardinality")).map(c -> Cardinality.fromValue(c.textValue())).orElse(Cardinality.ONE);
        register = Optional.ofNullable(node.get("register")).map(JsonNode::asText).filter(s -> !s.isEmpty() && !"null".equals(s));
    }

    public Field(String name) {
        this.name = name;
        this.frendlyName = WordUtils.capitalize(name);
        this.datatype = Datatype.DEFAULT;
        this.cardinality = Cardinality.ONE;
        this.register = Optional.empty();
    }

    public Field(String name, Cardinality cardinality) {
        this.name = name;
        this.frendlyName = WordUtils.capitalize(name);
        this.datatype = Datatype.DEFAULT;
        this.cardinality = cardinality;
        this.register = Optional.empty();
    }

    public Field(String name, String frendlyName, Datatype datatype, Cardinality cardinality, Optional<String> register) {
        this.name = name;
        this.frendlyName = frendlyName;
        this.datatype = datatype;
        this.cardinality = cardinality;
        this.register = register;
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

    public Optional<String> getRegister() {
        return register;
    }
}
