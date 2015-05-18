package functional.html;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import functional.ApplicationTests;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryPageTest extends ApplicationTests {

    @Test
    public void create_addsAnEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("test-register").setValueAttribute("Testregister key");
        htmlForm.getInputByName("name").setValueAttribute("Some name");
        htmlForm.getInputByName("key1").setValueAttribute("Some key1");
        htmlForm.getInputByName("key2").setValueAttribute("some key2");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/hash"));

        String resultJson = webClient.getPage(resultPage.getUrl()).getWebResponse().getContentAsString();
        assertFalse(resultJson.contains("submit"));
    }

    @Test
    public void create_returnsErrorWhenTryToNewEntryWithDuplicatePrimaryKey() throws IOException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"value1\",\"key2\":\"value2\"}";

        assertEquals(202, postJson("/create", json).getStatus());

        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("test-register").setValueAttribute("testregisterkey");
        htmlForm.getInputByName("name").setValueAttribute("Some name");
        htmlForm.getInputByName("key1").setValueAttribute("Some key1");
        htmlForm.getInputByName("key2").setValueAttribute("some key2");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertEquals("http://localhost:3333/ui/create", resultPage.getUrl().toString());
    }

    @Test
    public void create_validtesMissingValueAndRendersTheSamePage() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("test-register").setValueAttribute("");
        htmlForm.getInputByName("name").setValueAttribute("some name");
        htmlForm.getInputByName("key1").setValueAttribute("Some key1");
        htmlForm.getInputByName("key2").setValueAttribute("some key2");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/ui/create"));

        htmlForm = resultPage.getForms().get(0);

        assertThat(htmlForm.getElementsByAttribute("label", "for", "test-register_error").get(0).asText()).isEqualTo("Missing required key");
        assertThat(htmlForm.getInputByName("name").getValueAttribute()).isEqualTo("some name");
        assertThat(htmlForm.getInputByName("key1").getValueAttribute()).isEqualTo("Some key1");
        assertThat(htmlForm.getInputByName("key2").getValueAttribute()).isEqualTo("some key2");

    }

    @Test
    public void update_updatesTheEntryInTheRegister() throws IOException, JSONException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"Key1Value\",\"key2\":\"Key2Value\"}";
        Record record = new Record(json);
        postJson("/create", json);

        HtmlPage page = webClient.getPage(BASE_URL + "/ui/supersede/" + record.getHash());
        HtmlForm form = page.getForms().get(0);

        assertThat(form.getInputByName("test-register").getValueAttribute()).isEqualTo("testregisterkey");
        assertThat(form.getInputByName("name").getValueAttribute()).isEqualTo("entryName");
        assertThat(form.getInputByName("key1").getValueAttribute()).isEqualTo("Key1Value");
        assertThat(form.getInputByName("key2").getValueAttribute()).isEqualTo("Key2Value");

        form.getInputByName("name").setValueAttribute("entryName");
        form.getInputByName("key1").setValueAttribute("updated Some key1");
        form.getInputByName("key2").setValueAttribute("updated key2");

        HtmlPage resultPage = form.getInputByName("submit").click();

        String resultUrl = resultPage.getUrl().toString();
        assertTrue(resultUrl.startsWith(BASE_URL + "/hash/"));

        final String jsonResultUrl = resultUrl.replace("/hash/", "/hash.json/");
        String jsonResponse = webClient.getPage(jsonResultUrl).getWebResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"test-register\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"updated Some key1\",\"key2\":\"updated key2\"}",
                Json.parse(jsonResponse).get("entry").toString(),
                true);

    }

    @Test
    public void update_validatesMissingValueAndRendersTheSamePage() throws IOException {
        String json = "{\"test-register\":\"Testregisterkey\",\"name\":\"entryName\",\"key1\":\"Key1Value\",\"key2\":\"Key2Value\"}";
        Record record = new Record(json);
        postJson("/create", json);

        HtmlPage page = webClient.getPage(BASE_URL + "/ui/supersede/" + record.getHash());
        HtmlForm form = page.getForms().get(0);

        form.getInputByName("test-register").setValueAttribute("");
        form.getInputByName("name").setValueAttribute("entryName");
        form.getInputByName("key1").setValueAttribute("");
        form.getInputByName("key2").setValueAttribute("updated key2");

        HtmlPage resultPage = form.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/ui/supersede/" + record.getHash()));

        form = resultPage.getForms().get(0);

        assertThat(form.getInputByName("name").getValueAttribute()).isEqualTo("entryName");
        assertThat(form.getInputByName("key1").getValueAttribute()).isEqualTo("");
        assertThat(form.getElementsByAttribute("label", "for", "test-register_error").get(0).asText()).isEqualTo("Missing required key");
        assertThat(form.getInputByName("key2").getValueAttribute()).isEqualTo("updated key2");

    }
}
