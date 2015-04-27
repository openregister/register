package controllers.conf;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.config.ApplicationConf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Register {

    public final static List<String> keys = new ArrayList<>();

    public static void init() {
        String name = ApplicationConf.getString("register.name");
        String registersServiceUri = ApplicationConf.getString("registers.service.url");

        F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + name + "?_representation=json").execute();
        F.Promise<List<String>> listPromise = promise.map(r -> {
                    List<String> resultKeys = new ArrayList<>();
                    Iterator<JsonNode> elements = r.asJson().get("entry").get("fields").elements();
                    while (elements.hasNext()) {
                        resultKeys.add(elements.next().textValue());
                    }
                    return resultKeys;
                }
        );
        keys.addAll(listPromise.get(10000));
    }
}
