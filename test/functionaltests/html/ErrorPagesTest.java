package functionaltests.html;

import org.junit.Test;
import play.libs.ws.WSResponse;
import functionaltests.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;

public class ErrorPagesTest extends ApplicationTests {


    @Test
    public void testUnknownHash() throws Exception {

        WSResponse response = get("/hash/123");

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
        String body = response.getBody();
        assertThat(body).contains("<h1 class=\"error\">Entry not found</h1>");
    }
}
