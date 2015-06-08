package controllers.html;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.api.Representations;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.markdownj.MarkdownProcessor;
import play.twirl.api.Html;
import uk.gov.openregister.StreamUtils;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Datatype;
import uk.gov.openregister.model.Field;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class Utils {

    public static Html toRepresentationLinks(String uri) throws URISyntaxException {

        Representations.Format[] formats = Representations.Format.values();

        List<NameValuePair> queryParams = new URIBuilder(uri).getQueryParams();

        String uriWithoutRepresentation = uri.replaceAll("([^\\?]+)(.*)", "$1").replaceAll("(.*?)(\\.[a-z]+)?$", "$1");

        StringBuilder linksHtml = new StringBuilder("");

        for (Representations.Format format : formats) {
            URIBuilder uriBuilder = createUriWithFormat(uriWithoutRepresentation, format);
            queryParams.forEach(nvPair -> uriBuilder.setParameter(nvPair.getName(), nvPair.getValue()));
            linksHtml.append(String.format("<a href=\"%s\" rel=\"alternate\">%s</a>, ", uriBuilder.build().toString(), format.name()));
        }

        return Html.apply(linksHtml.toString().replaceAll(", $", "").replaceAll(",([^,]+)$", " and$1"));
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
        return toRegisterLink(registerName, registerName);
    }

    public static Html toRegisterLink(String registerName, String displayText) {
        String registerUri = ApplicationConf.getRegisterServiceTemplateUrl().replace("__REGISTER__", registerName);
        return Html.apply("<a class=\"link_to_register\" href=\"" + registerUri + "\">" + displayText + "</a>");
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
        } else if (field.getRegister().isPresent()) {
            return toLink(field.getRegister().get(), value.textValue()).text();
        } else if (field.getDatatype() == Datatype.CURIE) {
            Optional<Curie> curie = Curie.of(value.textValue());
            return curie.map(c -> toLink(c.namespace, c.identifier).text()).orElse(value.textValue());
        } else {
            return value.textValue();
        }
    }

    private static URIBuilder createUriWithFormat(String uriWithoutRepresentation, Representations.Format format) throws URISyntaxException {
        if (uriWithoutRepresentation.endsWith("search")) {
            URIBuilder uriBuilder = new URIBuilder(uriWithoutRepresentation);
            uriBuilder.setParameter("_representation", format.name());
            return uriBuilder;
        } else {
            return new URIBuilder(uriWithoutRepresentation + "." + format.name());
        }
    }
}
