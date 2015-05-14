package controllers.html;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import play.data.DynamicForm;
import play.twirl.api.Html;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Utils {

    public static Html toLink(Field field) {
        return toLink("field", field.getName());
    }

    public static Html toLink(Datatype datatype) {
        return toLink("datatype", datatype.getName());
    }

    public static Html toLink(String register, String name) {
        return Html.apply("<a class=\"link_to_register\" href=\"" + ApplicationConf.registerUrl(register, "/" + register + "/" + name) + "\">" + name + "</a>");
    }

    public static Html renderSelect(Field field, DynamicForm form, List<String> options) {
        String select = String.format("<label for=\"%s\">%s</label>", field.getName(), field.getName());
        select += String.format("<select id=\"%s\" name=\"%s\" multiple>", field.getName(), field.getName());

        for (String option : options) {
            String selected = "";
            if (form.get(field.getName()) != null) {
                if (Arrays.asList(form.get(field.getName()).split(",")).contains(option)) {
                    selected = "selected";
                }
            }

            select += String.format("<option value=\"%s\" %s>%s</option>", option, selected, option);
        }
        select += "</select>";
        return Html.apply(select);
    }

    public static Html toValue(Field field, JsonNode value) {
        return Html.apply(toRawValue(field, value));
    }

    private static String toRawValue(Field field, JsonNode value) {
        if (value == null) {
            return "";
        } else if (value.isArray()) {
            return "[ " + StringUtils.join(StreamUtils.asStream(value.elements()).map(e -> toRawValue(field, e)).collect(Collectors.toList()), ", ") + " ]";
        } else if (field.getRegister().isPresent()) return toLink(field.getRegister().get(), value.textValue()).text();
        else return value.textValue();
    }
}
