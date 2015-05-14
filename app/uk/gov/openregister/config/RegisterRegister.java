package uk.gov.openregister.config;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterRegister extends Register {

    private final List<Field> FIELDS;

    public RegisterRegister() {
        String fieldsUrl = ApplicationConf.registerUrl("field", "/search?_representation=json");
        WSResponse fr = WS.client().url(fieldsUrl).execute().get(30000);
        List<String> allowedValuesForFieldsField = new ArrayList<>();
        if (fr.getStatus() == 200) {
            allowedValuesForFieldsField = StreamUtils.asStream(fr.asJson().elements()).map(e -> e.get("entry").get("field").textValue()).collect(Collectors.toList());
        }

        FIELDS = Arrays.asList(
                new Field("register"),
                new Field("name"),
                new Field("fields", "Fields", Datatype.of("list"), Cardinality.MANY, Optional.of("field"), Optional.of(allowedValuesForFieldsField)),
                new Field("text")
        );
    }

    @Override
    public InitResult init() {
        return InitResult.OK;
    }

    @Override
    public String friendlyName() {
        return "Register";
    }

    @Override
    public String name() {
        return "register";
    }

    @Override
    public List<Field> fields() {
        return FIELDS;
    }

}
