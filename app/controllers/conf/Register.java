package controllers.conf;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static uk.gov.openregister.config.ApplicationConf.registerName;

public class Register {

    public static final Register instance = new Register();
    private Store store;
    private RegisterInfo registerInfo;
    private String friendlyName;

    public RegisterInfo registerInfo(){
        return registerInfo;
    }

    public Store store() {
        return store;
    }

    public void init() {

        if ("register".equalsIgnoreCase(registerName)) {
            List<String> keys = Arrays.asList(ApplicationConf.getString("registers.service.fields").split(","));
            registerInfo = new RegisterInfo(registerName, registerName.toLowerCase(), keys);
            friendlyName = "Register";
        } else {

            String registersServiceUri = ApplicationConf.getString("registers.service.url");

            F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + registerName + "?_representation=json").execute();

            WSResponse r = promise.get(30000);
            List<String> keys = new ArrayList<>();

            //TODO: If response is non 200, what do we want?
            if (r.getStatus() == 200) {
                JsonNode entry = r.asJson().get("entry");
                Iterator<JsonNode> elements = entry.get("fields").elements();
                while (elements.hasNext()) {
                    keys.add(elements.next().textValue());
                }

                friendlyName = entry.get("name").textValue();
            }
            registerInfo = new RegisterInfo(registerName, registerName.toLowerCase(), keys);
        }

        String uri = ApplicationConf.getString("store.uri");

        store = new PostgresqlStore(uri, registerInfo);
    }

    public String friendlyName() {
        return friendlyName;
    }
}
