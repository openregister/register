package functional.html;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.App;
import controllers.global.ApplicationGlobal;
import functional.ApplicationTests;
import functional.RegisterService;
import functional.TestSettings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.FakeApplication;
import play.test.Helpers;
import uk.gov.openregister.config.FieldRegister;
import uk.gov.openregister.domain.Record;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

public class FieldRegisterTest {
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
        return WS.url(BASE_URL + path).setFollowRedirects(false).get().get(ApplicationTests.TIMEOUT);
    }

    public WSResponse postJson(String path, String json) {
        return WS.url(BASE_URL + path)
                .setHeader("Content-Type", "application/json").post(json).get(ApplicationTests.TIMEOUT);
    }

    @Test
    public void testRegistersListIsLoaded() throws Exception {
        running(testServer(3334, fakeApplication("field")), () -> {
            assertThat(App.instance.register.friendlyName()).isEqualTo("Field");

            assertThat(App.instance.register instanceof FieldRegister).isEqualTo(true);

            FieldRegister fr = (FieldRegister) App.instance.register;

            List<JsonNode> entries = fr.getRegisters().stream().map(Record::getEntry).collect(Collectors.toList());
            assertThat(entries).contains(Json.parse("{\"register\": \"test-register\",\"name\": \"Test Register\", \"fields\":[\"test-register\", \"name\",\"key1\",\"key2\"]}"));
            assertThat(entries).contains(Json.parse("{\"register\": \"test-register-2\",\"name\": \"Test Register 2\", \"fields\":[\"test-register-2\", \"name\",\"key1\"]}"));
        });
    }


    @Test
    public void testEntryViewShowsTheListOfRegisters() throws Exception {
        running(testServer(3334, fakeApplication("field")), () -> {

            String json = "{\"field\":\"name\"}";
            postJson("/create", json);

            WSResponse response = get("/field/name");
            org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

            Element usage = html.getElementById("field-usage");
            assertThat(usage).isNotNull();

            assertThat(usage.toString()).contains("<a class=\"link_to_register\" href=\"http://localhost:8888\">test-register</a>");
            assertThat(usage.toString()).contains("<a class=\"link_to_register\" href=\"http://localhost:8888\">test-register-2</a>");

        });
    }

    @Test
    public void testUsageDoesNotShowOnOtherRegisters() throws Exception {

        running(testServer(3334, fakeApplication("some-register")), () -> {
            String json = "{\"some-register\":\"asd\",\"name\":\"The Name\"}";
            postJson("/create", json);

            WSResponse response = get("/some-register/asd");
            org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

            Element usage = html.getElementById("field-usage");
            assertThat(usage).isNull();
        });
    }
}
