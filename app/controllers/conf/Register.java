package controllers.conf;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.PostgresqlStore;
import uk.gov.openregister.store.postgresql.RegisterInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Register {

    public static final Register instance = new Register();
    private Store store;
    private List<String> fields;
    private String name;
    private String friendlyName;


    public Store store() {
        return store;
    }

    public void init() {
        name = ApplicationConf.getString("register.name");

        if ("register".equalsIgnoreCase(name)) {
            fields = Arrays.asList(ApplicationConf.getString("registers.service.fields").split(","));
            friendlyName = "Register";
        } else {

            String registersServiceUri = ApplicationConf.getString("registers.service.url");
            F.Promise<WSResponse> promise = WS.client().url(registersServiceUri + "/register/" + name + "?_representation=json").execute();
            WSResponse r = promise.get(30000);

            //TODO: If response is non 200, what do we want?
            if (r.getStatus() == 200) {
                JsonNode entry = r.asJson().get("entry");

                fields = StreamUtils.asStream(entry.get("fields").elements()).map(JsonNode::textValue).collect(Collectors.toList());
                friendlyName = entry.get("name").textValue();
            }

        }

        String uri = ApplicationConf.getString("store.uri");

        store = new PostgresqlStore(uri, new RegisterInfo(name, name.toLowerCase(), fields));
    }

    public String friendlyName() {
        return friendlyName;
    }

    public String name() {
        return name;
    }

    public List<String> fields() {
        return fields;
    }
}
