package uk.gov.openregister.linking;

import java.net.URI;
import java.net.URISyntaxException;

public class CurieResolver {
    private static final String REGISTER_TOKEN = "__REGISTER__";
    private static final String NAMESPACE_SEPARATOR = "_";
    private final String urlTemplate;

    public CurieResolver(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public URI resolve(Curie curie) {
        String register = getRegister(curie.namespace);
        String field = getField(curie.namespace);
        URI baseUri = URI.create(urlTemplate.replace(REGISTER_TOKEN, register));
        String path = String.format("/%s/%s", field, curie.identifier);
        try {
            return new URI(baseUri.getScheme(), baseUri.getAuthority(), path, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String getRegister(String namespace) {
        return namespace.split(NAMESPACE_SEPARATOR)[0];
    }

    private String getField(String namespace) {
        String[] namespaceParts = namespace.split(NAMESPACE_SEPARATOR);
        return namespaceParts[namespaceParts.length - 1];
    }
}
