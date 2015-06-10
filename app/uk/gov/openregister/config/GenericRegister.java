package uk.gov.openregister.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.text.WordUtils;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GenericRegister extends Register {

    public static final int TIMEOUT = 30000;
    private String name;
    private List<Field> fields;
    private String friendlyName;

    public GenericRegister(String name) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name).replace('-',' ');
        this.fields = Collections.singletonList(new Field(name));
        initialise();
    }

    private void initialise() {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
        String rrUrl = curieResolver.resolve(new Curie("register", name)) + ".json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200) {
            JsonNode rEntry = rr.asJson().get("entry");

            List<String> fieldNames = StreamUtils.asStream(rEntry.get("fields").elements()).map(JsonNode::textValue).collect(Collectors.toList());

            fields = fieldNames.stream().map(field -> {

                String frUrl = curieResolver.resolve(new Curie("field", field)) + ".json";
                WSResponse fr = WS.client().url(frUrl).execute().get(TIMEOUT);

                if (fr.getStatus() == 200) {
                    JsonNode fEntry = fr.asJson().get("entry");
                    return new Field(fEntry);
                } else {
                    throw new RuntimeException("Field register returned " + fr.getStatus() + " calling " + frUrl);
                }

            }).collect(Collectors.toList());
        } else {
            throw new RuntimeException("Register register returned " + rr.getStatus() + " calling " + rrUrl);
        }
    }

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

}
