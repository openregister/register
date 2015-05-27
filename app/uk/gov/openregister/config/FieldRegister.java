package uk.gov.openregister.config;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldRegister extends Register {

    public static final List<Field> FIELDS = Arrays.asList(new Field("field"), new Field("datatype"), new Field("register", Optional.of("register")), new Field("cardinality"), new Field("text", Datatype.TEXT));

    private List<Record> registers = new ArrayList<>();

    public FieldRegister() {
    }

    public FieldRegister(List<Record> registers) {
        this.registers = registers;
    }

    @Override
    public String friendlyName() {
        return "Field";
    }

    @Override
    public String name() {
        return "field";
    }

    @Override
    public List<Field> fields() {
        return FIELDS;
    }

    public List<Record> getRegisters() {
        return registers;
    }

    public List<String> getRegisterNamesFor(String fieldName) {
        if(registers.isEmpty()){
            loadRegisters();
        }
        return registers.stream().map(Record::getEntry)
                .filter(e -> e.has("fields") && StreamUtils.asStream(e.get("fields").elements()).anyMatch(f -> f.textValue().equals(fieldName)))
                .map(e -> e.get("register").textValue())
                .collect(Collectors.toList());
    }

    private void loadRegisters() {
        String rrUrl = ApplicationConf.registerUrl("register", "/search?_query=&_representation=json");
        try {
            WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

            registers = rr.asJson().findValues("entry").stream()
                    .map(Record::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Register register returned " + e.getMessage() + " calling " + rrUrl);
        }
    }
}
