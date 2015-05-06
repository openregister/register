package functional.html;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import functional.ApplicationTests;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryPageTest extends ApplicationTests {

    @Test
    public void create_addsAnEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("name").setValueAttribute("Some name");
        htmlForm.getInputByName("key1").setValueAttribute("Some key1");
        htmlForm.getInputByName("key2").setValueAttribute("some key2");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/hash/"));

        String resultJson = webClient.getPage(resultPage.getUrl() + "?_representation=json").getWebResponse().getContentAsString();
        assertFalse(resultJson.contains("submit"));
    }

    @Test
    public void create_validtesMissingValueAndRendersTheSamePage() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("name").setValueAttribute("");
        htmlForm.getInputByName("key1").setValueAttribute("Some key1");
        htmlForm.getInputByName("key2").setValueAttribute("some key2");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/ui/create"));

        htmlForm = resultPage.getForms().get(0);

        assertThat(htmlForm.getInputByName("name").getValueAttribute()).isEqualTo("");
        assertThat(htmlForm.getElementsByAttribute("dl", "id", "name_field").get(0).getElementsByAttribute("dd", "class", "error").get(0).asText()).isEqualTo("This field is required");
        assertThat(htmlForm.getInputByName("key1").getValueAttribute()).isEqualTo("Some key1");
        assertThat(htmlForm.getInputByName("key2").getValueAttribute()).isEqualTo("some key2");

    }
}
