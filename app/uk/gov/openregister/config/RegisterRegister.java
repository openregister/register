package uk.gov.openregister.config;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegisterRegister extends Register {

    private final List<Field> FIELDS = Arrays.asList(
            new Field("register"),
            new Field("fields", "Fields", Datatype.of("list"), Cardinality.MANY, Optional.of("field")),
            new Field("text", Datatype.TEXT),
            new Field("registry", Optional.of("public-body")),
            new Field("copyright", Datatype.TEXT),
            new Field("crest", Datatype.STRING)
    );

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
        Field fieldsField = FIELDS.stream().filter(f -> f.getName().equals("fields")).findFirst().get();

        if (!fieldsField.getAllowedValues().isPresent()) {

            String fieldsUrl = ApplicationConf.registerUrl("field", "/search?_representation=json");

            WSResponse fr = WS.client().url(fieldsUrl).execute().get(TIMEOUT);

            if (fr.getStatus() == 200) {
                fieldsField.setAllowedValues(Optional.of(StreamUtils.asStream(fr.asJson().get("entries").elements()).map(e -> e.get("entry").get("field").textValue()).collect(Collectors.toList())));
            }
        }

        return FIELDS;
    }

}
