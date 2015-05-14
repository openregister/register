package uk.gov.openregister.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import play.libs.Json;

import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class FieldTest {

    @Test
         public void testAFieldIsCreatedFromAJsonNode() throws Exception {
        JsonNode node = Json.parse("{\"name\":\"Address\",\"text\":\"A place in the UK with a postal address.\",\"field\":\"address\",\"datatype\":\"string\",\"register\":\"address\",\"cardinality\":null}");

        Field f = new Field(node);
        assertThat(f.getName()).isEqualTo("address");
        assertThat(f.getFriendlyName()).isEqualTo("Address");
        assertThat(f.getDatatype().getName()).isEqualTo("string");
        assertThat(f.getRegister()).isEqualTo(Optional.of("address"));
        assertThat(f.getCardinality()).isEqualTo(Cardinality.ONE);
    }

    @Test
    public void testAFieldIsCreatedFromAJsonNodeWithMissingValues() throws Exception {
        JsonNode node = Json.parse("{\"field\":\"address\"}");

        Field f = new Field(node);
        assertThat(f.getName()).isEqualTo("address");
        assertThat(f.getFriendlyName()).isEqualTo("Address");
        assertThat(f.getDatatype().getName()).isEqualTo("string");
        assertThat(f.getRegister().isPresent()).isEqualTo(false);
    }

    @Test
    public void testAFieldIsCreatedFromAJsonNodeWithNullValues() throws Exception {
        JsonNode node = Json.parse("{\"field\":\"address\",\"register\":\"null\"}");

        Field f = new Field(node);
        assertThat(f.getName()).isEqualTo("address");
        assertThat(f.getRegister().isPresent()).isEqualTo(false);
    }

    @Test
    public void testAFieldIsCreatedFromAJsonNodeWithEmptyValues() throws Exception {
        JsonNode node = Json.parse("{\"field\":\"address\",\"register\":\"\"}");

        Field f = new Field(node);
        assertThat(f.getName()).isEqualTo("address");
        assertThat(f.getRegister().isPresent()).isEqualTo(false);
    }

    @Test
    public void testAFieldReadsCardinality() throws Exception {
        JsonNode node = Json.parse("{\"field\":\"address\",\"cardinality\":\"n\"}");

        Field f = new Field(node);
        assertThat(f.getName()).isEqualTo("address");
        assertThat(f.getCardinality()).isEqualTo(Cardinality.MANY);
    }
}
