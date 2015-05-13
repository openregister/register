package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.*;
import uk.gov.openregister.model.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class App {

    public static App instance = new App();

    public static final int TIMEOUT = 30000;

    public Register register;

    private List<String> initErrors = new ArrayList<>();

    public void init() {

        initErrors.clear();

        String name = Optional.ofNullable(ApplicationConf.getString("register.name")).orElse("unknown-register");
        register = new UnInitialisedRegister(name);

        if ("register".equalsIgnoreCase(name)) {
            register = new RegisterRegister();
        } else if ("field".equalsIgnoreCase(name)) {
            register = new FieldRegister();
        } else if ("datatype".equalsIgnoreCase(name)) {
            register = new DatatypeRegister();
        } else {

            String rrUrl =  ApplicationConf.registerUrl("register", "/register/" + name + "?_representation=json");
            WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

            if (rr.getStatus() == 200 ) {
                JsonNode rEntry = rr.asJson().get("entry");

                List<String> fieldNames = StreamUtils.asStream(rEntry.get("fields").elements()).map(JsonNode::textValue).collect(Collectors.toList());

                List<Field> fields = fieldNames.stream().map(field -> {

                    String frUrl = ApplicationConf.registerUrl("field", "/field/" + field + "?_representation=json");
                    WSResponse fr = WS.client().url(frUrl).execute().get(TIMEOUT);

                    if (fr.getStatus() == 200) {
                        JsonNode fEntry = fr.asJson().get("entry");
                        return new Field(fEntry);
                    } else {
                        initErrors.add("Field register returned " + fr.getStatus() + " calling " + frUrl);
                        return new Field("unknown");
                    }

                }).collect(Collectors.toList());

                String friendlyName = rEntry.get("name").textValue();

                if(initErrors.isEmpty()) {
                    register = new Register() {
                        @Override
                        public String friendlyName() {
                            return friendlyName;
                        }

                        @Override
                        public String name() {
                            return name;
                        }

                        @Override
                        public List<Field> fields() {
                            return fields;
                        }

                        @Override
                        public boolean isStarted() {
                            return true;
                        }
                    };
                }
            } else {
                initErrors.add("Register register returned " + rr.getStatus() + " calling " + rrUrl);
            }

        }
    }

    public boolean started() {
        return register.isStarted();
    }

    public List<String> getInitErrors() {
        return initErrors;
    }

}
