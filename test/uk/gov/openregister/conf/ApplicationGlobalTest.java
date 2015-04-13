package uk.gov.openregister.conf;

import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;
import uk.gov.openregister.config.ApplicationGlobal;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

public class ApplicationGlobalTest extends ApplicationTests {

    @Test
    public void test404ErrorResponse() throws Exception {
        running(testServer(PORT, fakeApplication(new ApplicationGlobal())), () -> {
            WSResponse response = postJson("/idonotexist", "{}");
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getBody()).isEqualTo("{\"status\":404,\"message\":\"Action not found\"}");
        });
    }

    @Test
    public void test400ErrorResponse() throws Exception {
        running(testServer(PORT, fakeApplication(new ApplicationGlobal())), () -> {
            WSResponse response = postJson("/create", "{");
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getBody()).isEqualTo("{\"status\":400,\"message\":\"Invalid Json\"}");
        });
    }

}
