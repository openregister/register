package uk.gov.openregister;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldProvider {
    public static final int TIMEOUT = 30000;

    public static List<Field> getFields(String registerName, BiFunction<Integer, String, Object> errorHandler) {
        // would be nice to cache some or all of these requests on the client side
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getString("registers.service.template.url"));
        String rrUrl =  curieResolver.resolve(new Curie("register", registerName)) + ".json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200 ) {
            JsonNode rEntry = rr.asJson().get("entry");

            Stream<String> fieldNames = StreamUtils.asStream(rEntry.get("fields").elements()).map(JsonNode::textValue);

            return fieldNames.flatMap(field -> {

                String frUrl = curieResolver.resolve(new Curie("field", field)) + ".json";
                WSResponse fr = WS.client().url(frUrl).execute().get(TIMEOUT);

                if (fr.getStatus() == 200) {
                    JsonNode fEntry = fr.asJson().get("entry");
                    return Stream.of(new Field(fEntry));
                } else {
                    errorHandler.apply(fr.getStatus(), frUrl);
                    return Stream.empty();
                }

            }).collect(Collectors.toList());
        } else {
            errorHandler.apply(rr.getStatus(), rrUrl);
            return Collections.emptyList();
        }
    }
}
