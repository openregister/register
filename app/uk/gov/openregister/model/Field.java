package uk.gov.openregister.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;
import java.util.Optional;

public class Field {

    String name;
    String friendlyName;
    Datatype datatype = Datatype.DEFAULT;
    Cardinality cardinality = Cardinality.ONE;
    Optional<String> register = Optional.empty();
    Optional<List<String>> allowedValues = Optional.empty();


    public Field(JsonNode node) {

        name = Optional.of(node.get("field")).map(JsonNode::asText).get();
        friendlyName = Optional.ofNullable(node.get("name")).map(JsonNode::asText).orElse(WordUtils.capitalize(name));
        datatype = Optional.ofNullable(node.get("datatype")).map(d -> Datatype.of(d.asText())).orElse(Datatype.DEFAULT);
        cardinality = Optional.ofNullable(node.get("cardinality")).map(c -> Cardinality.fromValue(c.textValue())).orElse(Cardinality.ONE);
        register = Optional.ofNullable(node.get("register")).map(JsonNode::asText).filter(s -> !s.isEmpty() && !"null".equals(s));
        this.allowedValues = Optional.empty();
    }

    public Field(String name) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name);
        this.datatype = Datatype.DEFAULT;
        this.cardinality = Cardinality.ONE;
        this.register = Optional.empty();
        this.allowedValues = Optional.empty();
    }

    public Field(String name, Cardinality cardinality) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name);
        this.cardinality = cardinality;
        this.register = Optional.empty();
        this.allowedValues = Optional.empty();
    }

    public Field(String name, Optional<String> register) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name);
        this.register = register;
    }

    public Field(String name, String friendlyName, Datatype datatype, Cardinality cardinality, Optional<String> register) {
        this.name = name;
        this.friendlyName = friendlyName;
        this.datatype = datatype;
        this.cardinality = cardinality;
        this.register = register;
        this.allowedValues = Optional.empty();
    }

    public Field(String name, String friendlyName, Datatype datatype, Cardinality cardinality, Optional<String> register, Optional<List<String>> allowedValues) {
        this.name = name;
        this.friendlyName = friendlyName;
        this.datatype = datatype;
        this.cardinality = cardinality;
        this.register = register;
        this.allowedValues = allowedValues;
    }

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
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

    public void setAllowedValues(Optional<List<String>> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public Optional<List<String>> getAllowedValues() {
        return allowedValues;
    }
}
