package uk.gov.openregister.linking;

import org.junit.Test;

import java.net.URI;

import static org.fest.assertions.Assertions.assertThat;

public class CurieResolverTest {
    @Test
    public void shouldResolveCurieBasedOnTemplate() throws Exception {
        CurieResolver resolver = new CurieResolver("http://__REGISTER__.register.elbonia");

        URI widgetUri = resolver.resolve(new Curie("widget", "1234"));

        assertThat(widgetUri).isEqualTo(URI.create("http://widget.register.elbonia/widget/1234"));
    }

    @Test
    public void shouldResolveCurieBasedOnTemplateWithExplicitPort() throws Exception {
        CurieResolver resolver = new CurieResolver("http://__REGISTER__.register.elbonia:5555");

        URI widgetUri = resolver.resolve(new Curie("widget", "1234"));

        assertThat(widgetUri).isEqualTo(URI.create("http://widget.register.elbonia:5555/widget/1234"));
    }

    @Test
    public void shouldEscapeSpacesInCurieValue() throws Exception {
        CurieResolver resolver = new CurieResolver("http://__REGISTER__.register.elbonia");

        URI widgetUri = resolver.resolve(new Curie("widget", "ABC DEF"));

        assertThat(widgetUri).isEqualTo(URI.create("http://widget.register.elbonia/widget/ABC%20DEF"));
    }

}
