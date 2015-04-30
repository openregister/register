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

    public List<String> keys() {
        return keys;
    }

    public Store store() {
        return store;
    }

    public void init() {
        String name = ApplicationConf.getString("register.name");

        if ("register".equalsIgnoreCase(name)) {
            keys = Arrays.asList(ApplicationConf.getString("registers.service.fields").split(","));
        } else {

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
            keys = listPromise.get(10000);
        }

        String uri = ApplicationConf.getString("store.uri");

        if (uri.startsWith("mongodb")) store = new MongodbStore(uri, name, Register.instance.keys);
        else if (uri.startsWith("postgres")) store = new PostgresqlStore(uri, name, Register.instance.keys);
        else throw new RuntimeException("Unable to find store for store.uri=" + uri);
    }
}
