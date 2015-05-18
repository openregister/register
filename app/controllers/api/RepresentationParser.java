package controllers.api;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepresentationParser {
    private final String REPRESENTATION_QUERY_PARAM = "_representation";
    private final Pattern representationQueryParamPattern = Pattern.compile(REPRESENTATION_QUERY_PARAM + "=([^=&]*)");

    public Optional<Representations.Format> formatForQuery(String uri) {
        final Matcher matcher = representationQueryParamPattern.matcher(uri);
        if(matcher.find()) {
            final String representation = matcher.group(1);
            return Optional.of(Representations.Format.valueOf(representation));
        } else {
            return Optional.empty();
        }
    }

    public Map<String, String> linksMap(String uri) {
        final Map<String, String> representationMap = new HashMap<>();
        final String uriPathSeparator = "/";
        final String representationSeparator = ".";

        try {
            URIBuilder uriParserBuilder = new URIBuilder(uri);
            final String uriPath = uriParserBuilder.getPath().replaceFirst(uriPathSeparator, "");
            final List<NameValuePair> uriQueryParams = uriParserBuilder.getQueryParams();

            // Assume that it is the first part of the path that defines the type of response
            for(Representations.Format format : Representations.Format.values()) {
                final String[] uriPathParts = uriPath.split(uriPathSeparator);

                final String uriPathFirstPart = uriPathParts[0];
                if(uriPathFirstPart.contains(representationSeparator)) {
                    int dotIdx = uriPathFirstPart.lastIndexOf(representationSeparator);
                    uriPathParts[0] = uriPathFirstPart.substring(0, dotIdx) + representationSeparator + format.identifier;
                } else {
                    uriPathParts[0] = uriPathFirstPart + representationSeparator + format.identifier;
                }

                representationMap.put(format.name(), buildUri(uriPathSeparator, uriQueryParams, uriPathParts));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri: " + uri, e);
        }

        return representationMap;
    }

    private String buildUri(String uriPathSeparator, List<NameValuePair> uriQueryParams, String[] uriPathParts)
            throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setPath(uriPathSeparator + String.join(uriPathSeparator, uriPathParts));
        uriBuilder.setParameters(uriQueryParams);
        return uriBuilder.build().toString();
    }
}
