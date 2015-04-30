/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package functional;

import com.gargoylesoftware.htmlunit.WebClient;
import controllers.conf.ApplicationGlobal;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.TestServer;
import helper.PostgresqlStoreForTesting;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static play.test.Helpers.testServer;

public class ApplicationTests {

    public static final int PORT = 3333;
    public static final String BASE_URL = "http://localhost:" + PORT;
    public static final long TIMEOUT = 10000L;
    public static final String REGISTER = "test_register";

    private static TestServer server;

    public WebClient webClient = new WebClient();

    public WSResponse postJson(String path, String json) {
        return WS.url(BASE_URL + path)
                .setHeader("Content-Type", "application/json").post(json).get(TIMEOUT);
    }

    public WSResponse getByKV(String key, String value, String representation) {
        return get("/" + key + "/" + value + "?_representation=" + representation);
    }

    public WSResponse getByHash(String hash, String representation) {
        return get("/hash/" + hash + "?_representation=" + representation);
    }

    public WSResponse search(String key, String value, String representation) {
        return get("/search?_representation=" + representation + "&" + key + "=" + value);
    }

    public WSResponse load(String url) {
        return get("/load?url=" + url);
    }

    public WSResponse get(String path) {
        return WS.url(BASE_URL + path).get().get(TIMEOUT);
    }

    @Before
    public void setUp() throws Exception {
        PostgresqlStoreForTesting.dropTable(REGISTER);
        PostgresqlStoreForTesting.createTable(REGISTER);
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
                TestSettings.forRegister(REGISTER),
                new ArrayList<>(),
                new ApplicationGlobal()
        );
    }

}
