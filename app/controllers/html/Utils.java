package controllers.html;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.markdownj.MarkdownProcessor;
import play.twirl.api.Html;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Utils {

    public static Html toRepresentationLinks(Map<String, String> representationMap) {
        final StringBuilder linksHtml = new StringBuilder("");

        representationMap.forEach((k,v) -> linksHtml.append(String.format("<a href=\"%s\" rel=\"alternate\">%s</a>,", v, k)));

        return Html.apply(linksHtml.toString().replaceAll(",$", "").replaceAll(",([^,]+)$", " and$1"));
    }

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
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
        return curieResolver.resolve(new Curie(register, value));
    }

    public static Html toRegisterLink(String registerName) {
        String registerUri = ApplicationConf.getRegisterServiceTemplateUrl().replace("__REGISTER__", registerName);
        return Html.apply("<a class=\"link_to_register\" href=\"" + registerUri + "\">" + registerName + "</a>");
    }


    public static Html checkbox(Field field, List<String> checkedElements, List<String> options) {
        String html = "<fieldset>";
        html += "<legend class=\"form-label-bold\">Fields</legend>";
        for (String option : options) {
            String checked = (checkedElements == null) ? "" : (checkedElements.contains(option) ? "checked" : "");
            html += String.format("<label class=\"block-label\" for=\"%s\">", option);
            html += String.format("<input id=\"%s\" name=\"%s\" type=\"checkbox\" value=\"%s\" %s>", option, field.getName(), option, checked);
            html += String.format("<span class=\"field-label\">%s</span></label>", option);


        }
        html += "</fieldset>";
        return Html.apply(html);
    }

    public static Html inputText(String name, List<String> value, String error) {
        String html = "";
        html += String.format("<label for=\"%s\">%s</label>", name, name);
        html += String.format("<input type=\"text\" id=\"%s\" name=\"%s\" value=\"%s\">", name, name, (value == null || value.isEmpty()) ? "" : value.get(0));
        if (!StringUtils.isEmpty(error)) {
            html += String.format("<label for=\"%s\">%s</label>", name + "_error", error);
        }
        return Html.apply(html);
    }

    public static Html toValue(Field field, JsonNode value) {
        if (field.getDatatype() == Datatype.TEXT) {
            return Html.apply(new MarkdownProcessor().markdown(toRawValue(field, value)));
        } else {
            return Html.apply(toRawValue(field, value));
        }
    }

    public static String join(List<String> list) {
        return "[ " + StringUtils.join(list, ", ") + " ]";
    }

    public static boolean isDisplayField(String fieldName, String register) {
        if (fieldName.equalsIgnoreCase(register)) {
            return true;
        }
        if (fieldName.equalsIgnoreCase("hash")) {
            return true;
        }
        // Alpha quick hack to pick some other displayable field.
        // This may be something that moves to fields or config?
        if (fieldName.equalsIgnoreCase("name") || fieldName.equalsIgnoreCase("street")) {
            return true;
        }
        return false;
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
