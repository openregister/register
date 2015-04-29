package controllers.json;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.bson.Document;
import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.ACCEPTED;
import static play.test.Helpers.BAD_REQUEST;

public class CreateRecordTest extends ApplicationTests {

    @Test
    public void testCreateARecordReturns202() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);
    }

    @Test
    public void testCreateARecordWithMalformedRequestReturns400() {
        String json = "this is not json";
        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testCreateARecordStoresItToTheDatabase() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        WSResponse response = postJson("/create", json);
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);

        Document document = collection().find().first();

        assertThat(document.get("entry")).isNotNull();
        assertThat(document.get("entry", Document.class).get("key1")).isEqualTo("value1");
        assertThat(document.get("entry", Document.class).get("key2")).isEqualTo("value2");
    }

    @Test
    public void addANewEntryInTheRegister() throws IOException {
        HtmlPage page = webClient.getPage(BASE_URL + "/create");
        HtmlForm htmlForm = page.getForms().get(0);

        htmlForm.getInputByName("school").setValueAttribute("Some school");
        htmlForm.getInputByName("name").setValueAttribute("Some name");
        htmlForm.getInputByName("startDate").setValueAttribute("Some startdate");
        htmlForm.getInputByName("website").setValueAttribute("website");

        HtmlPage resultPage = htmlForm.getInputByName("submit").click();
        assertTrue(resultPage.getUrl().toString().startsWith(BASE_URL + "/hash/"));
    }
}