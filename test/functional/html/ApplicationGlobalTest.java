package functional.html;

import org.junit.Test;
import play.libs.ws.WSResponse;
import functional.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;

public class ApplicationGlobalTest extends ApplicationTests {



    @Test
    public void test404ErrorResponse() throws Exception {
        WSResponse response = postJson("/idonotexist", "{}");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getBody()).contains("<h1 class=\"error\">Page not found</h1>");
    }

    @Test
    public void test400ErrorResponse() throws Exception {
        WSResponse response = postJson("/create", "{");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getBody()).contains("<h1 class=\"error\">Invalid Json</h1>");
    }

}
