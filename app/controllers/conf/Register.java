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

    public RegisterInfo registerInfo(){
        return registerInfo;
    }

    public Store store() {
        return store;
    }

    public void init() {

        if ("register".equalsIgnoreCase(registerName)) {
            registerInfo = new RegisterInfo(registerName, registerName.toLowerCase(), Arrays.asList(ApplicationConf.getString("registers.service.fields").split(",")));
        } else {

            String registersServiceUri = ApplicationConf.getString("registers.service.url");

            F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + registerName + "?_representation=json").execute();
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
            registerInfo = new RegisterInfo(registerName, registerName.toLowerCase(), listPromise.get(10000));
        }

        String uri = ApplicationConf.getString("store.uri");

        store = new PostgresqlStore(uri, registerInfo);
    }
}
