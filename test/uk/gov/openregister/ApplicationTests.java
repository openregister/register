package uk.gov.openregister;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;

public class ApplicationTests {

    public static final int PORT = 3333;

    public WSResponse postJson(String path, String json) {
        return WS.url("http://localhost:" + PORT + path)
                .setHeader("Content-Type", "application/json").post(json).get(10000L);
    }
}
