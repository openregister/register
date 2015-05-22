package functional.json;

import org.junit.Test;
import play.libs.ws.WSResponse;
import functional.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;

public class ApplicationGlobalTest extends ApplicationTests {

    @Test
    public void test404ErrorResponse() throws Exception {
        WSResponse response = postJson("/idonotexist", "{}");
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void test400ErrorResponse() throws Exception {
        WSResponse response = postJson("/create?_representation=json", "{");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeader("Content-type")).contains("text/html");
    }

}
