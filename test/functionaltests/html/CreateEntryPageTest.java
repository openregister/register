package functionaltests.html;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import functionaltests.ApplicationTests;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreateEntryPageTest extends ApplicationTests {

    @Test
    public void addNewEntry_ignoresTheExtraFormFieldsAndCreatesANewEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("school").setValueAttribute("Some school");
        htmlForm.getInputByName("name").setValueAttribute("Some name");
        htmlForm.getInputByName("startDate").setValueAttribute("Some startdate");
        htmlForm.getInputByName("website").setValueAttribute("website");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/hash/"));

        String resultJson = webClient.getPage(resultPage.getUrl() + "?_representation=json").getWebResponse().getContentAsString();
        assertFalse(resultJson.contains("submit"));
    }
}
