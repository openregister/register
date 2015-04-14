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
import uk.gov.openregister.config.ApplicationGlobal;
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

    public WSResponse getByKV(String key, String value) {
        return WS.url("http://localhost:" + PORT + "/" + key + "/" + value).get().get(TIMEOUT);
    }

    public WSResponse getByHash(String hash) {
        return WS.url("http://localhost:" + PORT + "/hash/" + hash).get().get(TIMEOUT);
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
