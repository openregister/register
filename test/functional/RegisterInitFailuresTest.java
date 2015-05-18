package functional;

import controllers.App;
import controllers.global.ApplicationGlobal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.FakeApplication;
import play.test.Helpers;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

public class RegisterInitFailuresTest {

    public static final String BASE_URL = "http://localhost:" + 3334;

    @BeforeClass
    public static void startApp() throws Exception {
        RegisterService.start();
    }

    @AfterClass
    public static void stopApp() throws Exception {
        RegisterService.stop();
    }

    @After
    public void tearDown() throws Exception {

        // the static instance is the same from one test to another...
        App.instance = new App();
    }

    private static FakeApplication fakeApplication(String registerName) throws URISyntaxException {
        String applicationRoot = new File(ApplicationGlobal.class.getResource("/").toURI())
                .getAbsolutePath()
                .replaceAll("/target/scala.*", "");

        return new FakeApplication(
                new File(applicationRoot),
                Helpers.class.getClassLoader(),
                TestSettings.forRegister(registerName),
                new ArrayList<>(),
                new ApplicationGlobal()
        );
    }

    public WSResponse get(String path) {
        return WS.url(BASE_URL + path).setFollowRedirects(false).get().get(1000);
    }

    @Test
    public void testRegisterStartsWhenInitialisationFails() throws Exception {
        running(testServer(3334, fakeApplication("test-register-that-doesnt-exist")), () -> {
            assertThat(App.instance.started()).isEqualTo(false);
            assertThat(App.instance.getInitErrors()).containsExactly("Register register returned 404 calling http://localhost:8888/register.json/test-register-that-doesnt-exist");
            assertThat(App.instance.register.name()).isEqualTo("test-register-that-doesnt-exist");
        });
    }


    @Test
    public void testAllExistingRoutesShowTheErrorsWhenNotInititalised() throws Exception {
        running(testServer(3334, fakeApplication("test-register-that-doesnt-exist")), () -> {
            String body = get("/").getBody();
            assertThat(body).contains("Test-register-that-doesnt-exist Register bootstrap failed");
            assertThat(body).contains("Register register returned 404 calling http://localhost:8888/register.json/test-register-that-doesnt-exist");
        });
    }

    @Test
    public void testShowErrorWhenFieldsAreNotFound() throws Exception {
        running(testServer(3334, fakeApplication("test-register-with-fields-that-dont-exist")), () -> {
            String body = get("/").getBody();
            assertThat(body).contains("Test Register With Unknown Fields Register bootstrap failed");
            assertThat(body).contains("Field register returned 404 calling http://localhost:8888/field.json/unknown");
        });
    }

    @Test
    public void testKnownRegisterRegisterWorks() throws Exception {
        running(testServer(3334, fakeApplication("register")), () -> {
            assertThat(get("/").getStatus()).isEqualTo(200);
        });
    }

    @Test
    public void testKnownFieldRegisterWorks() throws Exception {
        running(testServer(3334, fakeApplication("field")), () -> {
            assertThat(get("/").getStatus()).isEqualTo(200);
        });
    }

    @Test
    public void testKnownDatatypeRegisterWorks() throws Exception {
        running(testServer(3334, fakeApplication("datatype")), () -> {
            assertThat(get("/").getStatus()).isEqualTo(200);
        });
    }

    @Test
    public void test_initTriggersAnotherBootstrap() throws Exception {
        running(testServer(3334, fakeApplication("datatype")), () -> {
            assertThat(get("/?_init=true").getStatus()).isEqualTo(303);
        });
    }

}
