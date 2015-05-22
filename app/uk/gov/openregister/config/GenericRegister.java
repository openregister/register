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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class GenericRegister extends Register {

    public static final int TIMEOUT = 30000;
    private String name;
    private List<Field> fields;
    private String friendlyName;

    public GenericRegister(String name) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name);
        this.fields = Collections.singletonList(new Field(name));
    }

    @Override
    public InitResult init() {

        InitResult result = new InitResult(false);

        fields = getFields(name, (status, url) -> result.errors().add("Error: got status " + status + " calling " + url));
        if(result.errors.isEmpty()) result.started = true;
        return result;
    }

    public static List<Field> getFields(String registerName, BiFunction<Integer, String, Object> errorHandler) {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getString("registers.service.template.url"));
        String rrUrl =  curieResolver.resolve(new Curie("register", registerName)) + ".json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200 ) {
            JsonNode rEntry = rr.asJson().get("entry");

            List<String> fieldNames = StreamUtils.asStream(rEntry.get("fields").elements()).map(JsonNode::textValue).collect(Collectors.toList());

            return fieldNames.stream().map(field -> {

                String frUrl = curieResolver.resolve(new Curie("field", field)) + ".json";
                WSResponse fr = WS.client().url(frUrl).execute().get(TIMEOUT);

                if (fr.getStatus() == 200) {
                    JsonNode fEntry = fr.asJson().get("entry");
                    return new Field(fEntry);
                } else {
                    errorHandler.apply(fr.getStatus(), frUrl);
                    return new Field("unknown");
                }

            }).collect(Collectors.toList());
        } else {
            errorHandler.apply(rr.getStatus(), rrUrl);
            return Collections.emptyList();
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
