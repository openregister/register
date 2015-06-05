package functional;

import controllers.html.Utils;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class UtilsTest extends ApplicationTests {

    public static final Field A_FIELD = new Field("aField");
    public static final Field FIELD_WITH_REGISTER = new Field("fields", "Field", Datatype.of("list"), Cardinality.ONE, Optional.of("field"));
    public static final Field FIELD_WITH_CURIE = new Field("person-or-company", "A person or a company", Datatype.CURIE, Cardinality.ONE, Optional.empty());

    @Test
    public void testRenderAStringValue() throws Exception {

        assertThat(Utils.toValue(A_FIELD, Json.parse("\"value\"")).text()).isEqualTo("value");
    }

    @Test
    public void testRenderALinkToRegister() throws Exception {

        assertThat(Utils.toValue(FIELD_WITH_REGISTER, Json.parse("\"value\"")).text()).isEqualTo("<a class=\"link_to_register\" href=\"http://localhost:8888/field/value\">value</a>");
    }

    @Test
    public void testRenderACurie() throws Exception {

        assertThat(Utils.toValue(FIELD_WITH_CURIE, Json.parse("\"person-or-company:an-id\"")).text()).isEqualTo("<a class=\"link_to_register\" href=\"http://localhost:8888/person-or-company/an-id\">an-id</a>");
    }

    @Test
    public void testRenderALinkToAField() throws Exception {

        assertThat(Utils.toLink(A_FIELD).text()).isEqualTo("<a class=\"link_to_register\" href=\"http://localhost:8888/field/aField\">aField</a>");
    }

    @Test
    public void testRenderALinkToADatatype() throws Exception {

        assertThat(Utils.toLink(Datatype.STRING).text()).isEqualTo("<a class=\"link_to_register\" href=\"http://localhost:8888/datatype/string\">string</a>");
    }

    @Test
    public void testRenderAnArrayOfValues() throws Exception {

        assertThat(Utils.toValue(A_FIELD, Json.parse("[\"value1\",\"value2\"]")).text()).isEqualTo("[ value1, value2 ]");
    }

    @Test
    public void testRenderAnArrayOfLinks() throws Exception {

        assertThat(Utils.toValue(FIELD_WITH_REGISTER, Json.parse("[\"value1\",\"value2\"]")).text())
                .isEqualTo("[ <a class=\"link_to_register\" href=\"http://localhost:8888/field/value1\">value1</a>, " +
                        "<a class=\"link_to_register\" href=\"http://localhost:8888/field/value2\">value2</a> ]");
    }

    @Test
    public void testDisplayFieldIsTrueForHash() throws Exception {
        assertThat(Utils.isDisplayField("hash", "notcare")).isTrue();
    }

    @Test
    public void testDisplayFieldIsFalseForRandomField() throws Exception {
        assertThat(Utils.isDisplayField("banana", "notcare")).isFalse();
    }

    @Test
    public void testDisplayFieldIsTrueForRegisterKey() throws Exception {
        assertThat(Utils.isDisplayField("registername", "registername")).isTrue();
    }

    @Test
    public void testDisplayFieldIsTrueForNameField() throws Exception {
        assertThat(Utils.isDisplayField("name", "notcare")).isTrue();
    }

    @Test
    public void testDisplayFieldIsTrueForStreetField() throws Exception {
        assertThat(Utils.isDisplayField("street", "some-register")).isTrue();
    }


}
