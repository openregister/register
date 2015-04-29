package functional;

import by.stub.client.StubbyClient;

import java.io.File;

public class RegisterService {


    private static  StubbyClient stubbyClient = new StubbyClient();

    public static void start() throws Exception {
        String yamlResource = new File(RegisterService.class.getResource("/registers-service.yaml").toURI()).getAbsolutePath();
        stubbyClient.startJetty(8888, yamlResource);
    }

    public static void stop() throws Exception {
        stubbyClient.stopJetty();
    }
}
