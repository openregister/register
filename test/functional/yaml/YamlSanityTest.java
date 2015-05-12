package functional.yaml;

import functional.ApplicationTests;
import org.junit.Test;
import play.libs.ws.WSResponse;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class YamlSanityTest extends ApplicationTests {

    public static final String TEST_JSON = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
    public static final String EXPECTED_HASH = "4686f89b9c983f331c7deef476fda719148de4fb";
    public static final String EXPECTED_YAML = "---\n" +
            "hash: \"4686f89b9c983f331c7deef476fda719148de4fb\"\n" +
            "entry:\n" +
            "  key1: \"value1\"\n" +
            "  key2:\n" +
            "  - \"A\"\n" +
            "  - \"B\"\n" +
            "  name: \"The Entry\"\n" +
            "  test-register: \"testregisterkey\"\n";

    @Test
    public void testFindOneByKey() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByKV("key1", "value1", "yaml");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo("text/yaml; charset=utf-8");
        assertThat(response.getBody()).isEqualTo(EXPECTED_YAML);
    }

    @Test
    public void testFindOneByHash() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByHash(EXPECTED_HASH, "yaml");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo("text/yaml; charset=utf-8");
        assertThat(response.getBody()).isEqualTo(EXPECTED_YAML);
    }


    @Test
    public void test404ErrorResponse() throws Exception {
        WSResponse response = get("/idonotexist?_representation=yaml");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(
                "---\n" +
                        "message: \"Page not found\"\n" +
                        "errors: []\n" +
                        "status: 404\n");
    }
}
