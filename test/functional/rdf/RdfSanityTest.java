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
            "<http://localhost:8888/hash/4686f89b9c983f331c7deef476fda719148de4fb>\n" +
            "  field:test-register <http://localhost:8888/test-register/testregisterkey> ;\n" +
            "  field:name \"The Entry\" ;\n" +
            "  field:key1 \"value1\" ;\n" +
            "  field:key2 \"A\", \"B\" .\n";
    public static final String TEXT_TURTLE = "text/turtle; charset=utf-8";

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
        assertThat(response.getHeader("Content-type")).isEqualTo(TEXT_TURTLE);
        assertThat(response.getBody()).isEqualTo(EXPECTED_TURTLE);
    }

    public static final String EXPECTED_TURTLE_LIST = "@prefix field: <http://fields.openregister.org/field/>.\n" +
            "\n" +
            "<http://localhost:8888/hash/39837068f586ab19bcb2b5f2408b024438e75c43>\n" +
            "  field:test-register <http://localhost:8888/test-register/testregisterkey1> ;\n" +
            "  field:name \"The Entry1\" ;\n" +
            "  field:key1 \"value1\" ;\n" +
            "  field:key2 \"A\", \"B\" .\n" +
            "\n" +
            "<http://localhost:8888/hash/b0c762fd934019b14a3ec88d775c6a037a09a74e>\n" +
            "  field:test-register <http://localhost:8888/test-register/testregisterkey2> ;\n" +
            "  field:name \"The Entry2\" ;\n" +
            "  field:key1 \"value2\" ;\n" +
            "  field:key2 \"C\", \"D\" .\n";

    @Test
    public void testSearchAndRenderListOfResults() throws Exception {
        postJson("/create", "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"C\",\"D\"]}");

        WSResponse response = get("/search.turtle?_query=");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(TEXT_TURTLE);
        assertThat(response.getBody()).isEqualTo(EXPECTED_TURTLE_LIST);
    }
}
