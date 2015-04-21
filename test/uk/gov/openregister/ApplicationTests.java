package uk.gov.openregister;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.Helpers;
import play.test.TestServer;
import controllers.ApplicationGlobal;
import uk.gov.openregister.store.MongodbStoreForTesting;

import static play.test.Helpers.testServer;

public class ApplicationTests {

    public static final int PORT = 3333;
    public static final long TIMEOUT = 10000L;
    public static final String REGISTER = "test_register";
    private static TestServer server;

    public WSResponse postJson(String path, String json) {
        return WS.url("http://localhost:" + PORT + path)
                .setHeader("Content-Type", "application/json").post(json).get(TIMEOUT);
    }

    public WSResponse postFormUrlEncoded(String path, String body) {
        return WS.url("http://localhost:" + PORT + path)
                .setHeader("Content-Type", "application/x-www-form-urlencoded").post(body).get(TIMEOUT);
    }


    public WSResponse getByKV(String key, String value) {
        return get("/" + key + "/" + value);
    }

    public WSResponse getByHash(String hash) {
        return get("/hash/" + hash);
    }

    public WSResponse search(String key, String value, String representation) {
        return get("/search?_representation=" + representation + "&" + key + "=" + value);
    }

    public WSResponse load(String url) {
        return get("/load?url=" + url);
    }

    public WSResponse get(String path) {
        return WS.url("http://localhost:" + PORT + path).get().get(TIMEOUT);
    }

    @Before
    public void setUp() throws Exception {
        collection().drop();
    }

    protected MongoCollection<Document> collection() {
        return MongodbStoreForTesting.collection(REGISTER);
    }

    @BeforeClass
    public static void startApp() {
        server = testServer(PORT, Helpers.fakeApplication(MongodbStoreForTesting.settings(REGISTER),
                new ApplicationGlobal()));
        Helpers.start(server);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(server);
    }

}
