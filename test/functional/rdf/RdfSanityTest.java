package functional.rdf;

import functional.ApplicationTests;
import org.junit.Test;
import play.libs.ws.WSResponse;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class RdfSanityTest extends ApplicationTests {
    public static final String TEST_JSON = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
    public static final String EXPECTED_HASH = "4686f89b9c983f331c7deef476fda719148de4fb";

    public static final String EXPECTED_TURTLE = "@prefix field: <http://fields.openregister.org/field/>.\n" +
            "\n" +
            "<http://test-register.openregister.org/hash/4686f89b9c983f331c7deef476fda719148de4fb>\n" +
            "  field:test-register \"testregisterkey\" ;\n" +
            "  field:name \"The Entry\" ;\n" +
            "  field:key1 \"value1\" ;\n" +
            "  field:key2 \"A\", \"B\" .\n";

    @Test
    public void testFindOneByKey() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByKV("key1", "value1", "turtle");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo("text/turtle; charset=utf-8");
        assertThat(response.getBody()).isEqualTo(EXPECTED_TURTLE);
    }

    @Test
    public void testFindOneByHash() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByHash(EXPECTED_HASH, "turtle");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo("text/turtle; charset=utf-8");
        assertThat(response.getBody()).isEqualTo(EXPECTED_TURTLE);
    }
}
