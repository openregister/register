package functional;

import com.gargoylesoftware.htmlunit.WebClient;
import controllers.global.ApplicationGlobal;
import helper.PostgresqlStoreForTesting;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.TestServer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

import static play.test.Helpers.testServer;

public class ApplicationTests {

    public static final int PORT = 3333;
    public static final long TIMEOUT = 1000000L;
    public static final String REGISTER = "test-register";
    public static final String BASE_URL = String.format("http://localhost:%s", PORT);

    private static TestServer server;

    public WebClient webClient = new WebClient();

    public WSResponse postJson(String path, String json) {
        return WS.url(BASE_URL + path)
                .setHeader("Content-Type", "application/json").post(json).get(TIMEOUT);
    }

    public WSResponse getByKV(String key, String value, String representation) {
        try {
            return get("/" + key + "/" + URLEncoder.encode(value, "utf-8") + "." + representation);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public WSResponse getByHash(String hash, String representation) {
        return get("/hash/" + hash + "." + representation);
    }

    public WSResponse search(String key, String value, String representation) {
        return get("/search?_representation=" + representation + "&" + key + "=" + value);
    }

    public WSResponse get(String path) {
        return WS.url(BASE_URL + path).get().get(TIMEOUT);
    }

    @Before
    public void setUp() throws Exception {
        PostgresqlStoreForTesting.dropTables(REGISTER);
        PostgresqlStoreForTesting.createTables(REGISTER);
        webClient.getOptions().setRedirectEnabled(true);
    }

    @BeforeClass
    public static void startApp() throws Exception {
        RegisterService.start();
        server = testServer(PORT, fakeApplication());
        Helpers.start(server);
    }

    @AfterClass
    public static void stopApp() throws Exception {
        Helpers.stop(server);
        RegisterService.stop();
    }

    private static FakeApplication fakeApplication() throws URISyntaxException {
        String applicationRoot = new File(ApplicationGlobal.class.getResource("/").toURI())
                .getAbsolutePath()
                .replaceAll("/target/scala.*", "");

        return new FakeApplication(
                new File(applicationRoot),
                Helpers.class.getClassLoader(),
                TestSettings.settings(),
                new ArrayList<>(),
                new ApplicationGlobal()
        );
    }

}
