package uk.gov.openregister.config;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegisterRegister extends Register {

    private List<Field> FIELDS;

    @Override
    public InitResult init() {
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
