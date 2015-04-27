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

    public static final Register schema = new Register();

    private Register() {
    }

    private List<String> keys = new ArrayList<>();


    public List<String> keys() {
        if (keys.isEmpty()) {
            keys = fetchKeysFromService();
            return keys;
        }
        return keys;
    }

    //TODO: should not be called twice, can synchronized it, need more discussion before I do it
    private List<String> fetchKeysFromService() {
        String name = ApplicationConf.getString("register.name");
        String registersServiceUri = ApplicationConf.getString("registers.service.url");

        F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + name + "?_representation=json").execute();
        F.Promise<List<String>> listPromise = promise.map(r -> {
                    List<String> resultKeys = new ArrayList<>();

                    //TODO: If response is non 200, what do we want?
                    if (r.getStatus() == 200) {
                        Iterator<JsonNode> elements = r.asJson().get("entry").get("fields").elements();
                        while (elements.hasNext()) {
                            resultKeys.add(elements.next().textValue());
                        }
                    }
                    return resultKeys;
                }
        );
        return listPromise.get(10000);
    }
}
