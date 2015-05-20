package functional.html;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import functional.ApplicationTests;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class HomePageTest extends ApplicationTests {
    @Test
    public void create_addsAnEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL);
        HtmlElement deptElement = page.getBody().getElementsByAttribute("p", "class", "big-meta").get(0);
        HtmlElement lastUpdateElement = page.getBody().getElementsByAttribute("p", "class", "big-meta").get(1);

        assertThat(deptElement.asText()).isEqualTo("Test department");
        assertThat(lastUpdateElement.asText()).isEqualTo("18 May 2015");
    }
}
