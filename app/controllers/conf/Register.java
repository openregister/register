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

public class Register {

    public static final Register instance = new Register();
    private List<String> keys = new ArrayList<>();
    private Store store;
    private String name;
    private String friendlyName;

    public List<String> keys() {
        return keys;
    }

    public Store store() {
        return store;
    }

    public void init() {
        name = ApplicationConf.getString("register.name");

        if ("register".equalsIgnoreCase(name)) {
            keys = Arrays.asList(ApplicationConf.getString("registers.service.fields").split(","));
            friendlyName = "Register";
        } else {

            String registersServiceUri = ApplicationConf.getString("registers.service.url");

            F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + name + "?_representation=json").execute();

            WSResponse r = promise.get(30000);
            keys = new ArrayList<>();

            //TODO: If response is non 200, what do we want?
            if (r.getStatus() == 200) {
                JsonNode entry = r.asJson().get("entry");
                Iterator<JsonNode> elements = entry.get("fields").elements();
                while (elements.hasNext()) {
                    keys.add(elements.next().textValue());
                }

                friendlyName = entry.get("name").textValue();
            }
        }

        String uri = ApplicationConf.getString("store.uri");

        if (uri.startsWith("mongodb")) store = new MongodbStore(uri, name, Register.instance.keys);
        else if (uri.startsWith("postgres")) store = new PostgresqlStore(uri, name, Register.instance.keys);
        else throw new RuntimeException("Unable to find store for store.uri=" + uri);
    }

    public String name() {
        return name;
    }

    public String friendlyName() {
        return friendlyName;
    }
}
