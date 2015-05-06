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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryPageTest extends ApplicationTests {

    @Test
    public void create_addsAnEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/ui/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("testregister").setValueAttribute("Testregister key");
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

    @Test
    public void update_updatesTheEntryInTheRegister() throws IOException, JSONException {
        String json = "{\"testregister\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"Key1Value\",\"key2\":\"Key2Value\"}";
        Record record = new Record(json);
        postJson("/create", json);

        HtmlPage page = webClient.getPage(BASE_URL + "/ui/supersede/" + record.getHash());
        HtmlForm form = page.getForms().get(0);

        assertThat(form.getInputByName("testregister").getValueAttribute()).isEqualTo("testregisterkey");
        assertThat(form.getInputByName("name").getValueAttribute()).isEqualTo("entryName");
        assertThat(form.getInputByName("key1").getValueAttribute()).isEqualTo("Key1Value");
        assertThat(form.getInputByName("key2").getValueAttribute()).isEqualTo("Key2Value");

        form.getInputByName("name").setValueAttribute("entryName");
        form.getInputByName("key1").setValueAttribute("updated Some key1");
        form.getInputByName("key2").setValueAttribute("updated key2");

        HtmlPage resultPage = form.getInputByName("submit").click();

        String resultUrl = resultPage.getUrl().toString();
        assertTrue(resultUrl.startsWith(BASE_URL + "/hash/"));

        String jsonResponse = webClient.getPage(resultUrl + "?_representation=json").getWebResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"testregister\":\"testregisterkey\",\"name\":\"entryName\",\"key1\":\"updated Some key1\",\"key2\":\"updated key2\"}",
                Json.parse(jsonResponse).get("entry").toString(),
                true);

    }

    @Test
    public void update_validatesMissingValueAndRendersTheSamePage() throws IOException {
        String json = "{\"testregister\":\"Testregisterkey\",\"name\":\"entryName\",\"key1\":\"Key1Value\",\"key2\":\"Key2Value\"}";
        Record record = new Record(json);
        postJson("/create", json);

        HtmlPage page = webClient.getPage(BASE_URL + "/ui/supersede/" + record.getHash());
        HtmlForm form = page.getForms().get(0);

        form.getInputByName("name").setValueAttribute("entryName");
        form.getInputByName("key1").setValueAttribute("");
        form.getInputByName("key2").setValueAttribute("updated key2");

        HtmlPage resultPage = form.getInputByName("submit").click();

        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/ui/supersede/" + record.getHash()));

        form = resultPage.getForms().get(0);

        assertThat(form.getInputByName("name").getValueAttribute()).isEqualTo("entryName");
        assertThat(form.getInputByName("key1").getValueAttribute()).isEqualTo("");
        assertThat(form.getElementsByAttribute("dl", "id", "key1_field").get(0).getElementsByAttribute("dd", "class", "error").get(0).asText()).isEqualTo("This field is required");
        assertThat(form.getInputByName("key2").getValueAttribute()).isEqualTo("updated key2");

    }
}
