package controllers.conf;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.mongodb.MongodbStore;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static uk.gov.openregister.config.ApplicationConf.registerName;

public class Register {

    public static final Register instance = new Register();
    private List<String> keys = new ArrayList<>();
    private Store store;

    public List<String> keys() {
        return keys;
    }

    public Store store() {
        return store;
    }

    public void init() {

        if ("register".equalsIgnoreCase(registerName)) {
            keys = Arrays.asList(ApplicationConf.getString("registers.service.fields").split(","));
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
            keys = listPromise.get(10000);
        }

        String uri = ApplicationConf.getString("store.uri");

        if (uri.startsWith("mongodb")) store = new MongodbStore(uri, registerName, Register.instance.keys);
        else if (uri.startsWith("postgres")) store = new PostgresqlStore(uri, registerName, Register.instance.keys);
        else throw new RuntimeException("Unable to find store for store.uri=" + uri);
    }
}
