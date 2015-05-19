package controllers.html;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import play.twirl.api.Html;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


public class Utils {

    public static Html toLink(Field field) {
        return toLink("field", field.getName());
    }

    public static Html toLink(Datatype datatype) {
        return toLink("datatype", datatype.getName());
    }

    public static Html toLink(String register, String value) {
        URI uri = toUri(register, value);
        return Html.apply("<a class=\"link_to_register\" href=\"" + uri + "\">" + value + "</a>");
    }

    public static URI toUri(String register, String value) {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getString("registers.service.template.url"));
        return curieResolver.resolve(new Curie(register, value));
    }

    public static Html toRegisterLink(String registerName){
        String registerUri = ApplicationConf.getString("registers.service.template.url").replace("__REGISTER__", registerName);
        return Html.apply("<a class=\"link_to_register\" href=\"" + registerUri + "\">" + registerName + "</a>");
    }


    public static Html checkbox(Field field, List<String> checkedElements, List<String> options) {
        String html = String.format("<label for=\"%s\">%s</label>", field.getName(), field.getName());
        for (String option : options) {
            String checked = (checkedElements == null) ? "" : (checkedElements.contains(option) ? "checked" : "");
            html += String.format("<input type=\"checkbox\" name=\"%s\", value=\"%s\" %s>%s<br/>", field.getName(), option, checked, option);
        }
        return Html.apply(html);
    }

    public static Html inputText(String name, List<String> value, String error) {
        String html = String.format("<label for=\"%s\">%s</label>", name, name);
        html += String.format("<input type=\"text\" name=\"%s\", value=\"%s\">", name, (value == null || value.isEmpty()) ? "" : value.get(0));
        if (!StringUtils.isEmpty(error)) {
            html += String.format("<label for=\"%s\">%s</label>", name + "_error", error);
        }
        return Html.apply(html);
    }

    public static Html toValue(Field field, JsonNode value) {
        return Html.apply(toRawValue(field, value));
    }

    public static String join(List<String> list) {
        return "[ " + StringUtils.join(list, ", ")+ " ]";
    }

    private static String toRawValue(Field field, JsonNode value) {
        if (value == null) {
            return "";
        } else if (value.isArray()) {
            return join(StreamUtils.asStream(value.elements()).map(e -> toRawValue(field, e)).collect(Collectors.toList()));
        } else if (field.getRegister().isPresent()) return toLink(field.getRegister().get(), value.textValue()).text();
        else return value.textValue();
    }
}
