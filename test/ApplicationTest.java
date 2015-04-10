import org.junit.Test;
import play.twirl.api.Content;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;


public class ApplicationTest {


    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("registers");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Hello world");
    }


}
