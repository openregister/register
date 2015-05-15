package uk.gov.openregister.linking;

import java.net.URI;
import java.net.URISyntaxException;

public class CurieResolver {
    private static final String REGISTER_TOKEN = "__REGISTER__";
    private final String urlTemplate;

    public CurieResolver(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public URI resolve(Curie curie) {
        URI baseUri = URI.create(urlTemplate.replace(REGISTER_TOKEN, curie.register));
        String path = String.format("/%s/%s", curie.register, curie.identifier);
        try {
            return new URI(baseUri.getScheme(), baseUri.getHost(), path, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
