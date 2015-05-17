package functional.html;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import functional.ApplicationTests;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.ACCEPTED;
import static play.mvc.Http.Status.OK;

public class SearchPagesTest extends ApplicationTests {

    @Test
    public void testGetSearchPage() throws Exception {

        WSResponse response = get("/");

        assertThat(response.getStatus()).isEqualTo(OK);
        String body = response.getBody();
        assertThat(body).contains("The official register of test entries throughout the UK");
        assertThat(body).contains("This register exists as an accurate list of all test entries");
        assertThat(body).contains("Search register entries");

        assertThat(body).contains("<a class=\"link_to_register\" href=\"http://localhost:8888/field/test-register\">test-register</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/name\">name</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/key1\">key1</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/key2\">key2</a>");

    }

    @Test
    public void testGetSearchPageShowsTheTotalAmountOfEntries() throws Exception {
        postJson("/create","{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry\",\"key1\": \"value2\",\"key2\": [\"A\",\"B\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey3\",\"name\":\"The Entry\",\"key1\": \"value3\",\"key2\": [\"A\",\"B\"]}");


        WSResponse response = get("/");

        assertThat(response.getStatus()).isEqualTo(OK);
        String body = response.getBody();
        assertThat(body).contains("<p class=\"big-number\">3</p>");
    }

    @Test
    public void testSubmitSearchQueryAndReturnsListOfEntries() throws Exception {
        postJson("/create", "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"C\",\"D\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey3\",\"name\":\"The Entry3\",\"key1\": \"value1\",\"key2\": [\"E\",\"F\"]}");

        WSResponse response = get("/search?_query=value1");
        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element table = html.getElementById("entries");
        assertThat(table).isNotNull();
        Elements tr = table.select("tr");
        Elements th = tr.first().select("th");
        assertThat(th.get(0).text()).isEqualTo("hash");
        assertThat(th.get(1).text()).isEqualTo("test-register");
        assertThat(th.get(2).text()).isEqualTo("name");
        assertThat(th.get(3).text()).isEqualTo("key1");
        assertThat(th.get(4).text()).isEqualTo("key2");

        Elements td1 = tr.get(1).select("td");
        assertThat(td1.get(0).select("a").first().toString()).isEqualTo("<a href=\"/hash/39837068f586ab19bcb2b5f2408b024438e75c43\">3983706</a>");
        assertThat(td1.get(1).text()).isEqualTo("testregisterkey1");
        assertThat(td1.get(2).text()).isEqualTo("The Entry1");
        assertThat(td1.get(3).text()).isEqualTo("value1");
        assertThat(td1.get(4).text()).isEqualTo("[ A, B ]");

        Elements td2 = tr.get(2).select("td");
        assertThat(td2.get(0).select("a").first().toString()).isEqualTo("<a href=\"/hash/9dd019eb60715299711418bc7a3542e93a466f58\">9dd019e</a>");
        assertThat(td2.get(1).text()).isEqualTo("testregisterkey3");
        assertThat(td2.get(2).text()).isEqualTo("The Entry3");
        assertThat(td2.get(3).text()).isEqualTo("value1");
        assertThat(td2.get(4).text()).isEqualTo("[ E, F ]");
    }

    @Test
    public void testRenderFieldWithoutKeyValueAndAFieldWithValueIsAList() throws Exception {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String hash =new Record(json).getHash();
        postJson("/create", json);

        WSResponse response = get("/hash/" + hash);

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element dl = html.select("dl").first();
        assertThat(dl).isNotNull();

        Elements dt = dl.select("dt");
        Elements dd = dl.select("dd");

        assertThat(dt.get(0).text()).isEqualTo("hash");
        assertThat(dd.get(0).text()).isEqualTo(hash);

        assertThat(dt.get(1).text()).isEqualTo("test-register");
        assertThat(dd.get(1).text()).isEqualTo("testregisterkey");

        assertThat(dt.get(2).text()).isEqualTo("name");
        assertThat(dd.get(2).text()).isEqualTo("The Entry");

        assertThat(dt.get(3).text()).isEqualTo("key1");
        assertThat(dd.get(3).text()).isEqualTo("value1");

        assertThat(dt.get(4).text()).isEqualTo("key2");
        assertThat(dd.get(4).text()).isEqualTo("[ A, B ]");
    }


    @Test
    public void testRenderFieldAsListInEntries() throws Exception {
        postJson("/create", "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");

        WSResponse response = search("key1", "value1", "html");

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element key2 = html.getElementsByClass("key2").first();
        assertThat(key2).isNotNull();

        assertThat(key2.text()).isEqualTo("[ A, B ]");

    }

    @Test
    public void testEntryShowsNameIfPresent() throws Exception {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String hash =new Record(json).getHash();
        postJson("/create", json);

        WSResponse response = get("/hash/" + hash);

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element h1 = html.getElementById("entry_name");
        assertThat(h1).isNotNull();
        assertThat(h1.text()).isEqualTo("The Entry");
    }

    @Test
    public void viewEntry_searchByKeyValueRendersTheHistory() throws IOException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String hash =new Record(json).getHash();
        postJson("/create", json);

        assertThat(postJson("/supersede/" + hash, json.replace("value1", "new_value")).getStatus()).isEqualTo(ACCEPTED);

        HtmlPage page = webClient.getPage(BASE_URL + "/test-register/testregisterkey");

        DomNodeList<HtmlElement> tables = page.getBody().getElementsByTagName("table");

        assertThat(tables.size()).isEqualTo(1);
        assertThat(((HtmlTable)tables.get(0)).getRows().size()).isEqualTo(3);
    }

    @Test
    public void viewEntry_searchByHashRendersTheHistory() throws IOException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        String hash =new Record(json).getHash();
        postJson("/create", json);

        assertThat(postJson("/supersede/" + hash, json.replace("value1", "new_value")).getStatus()).isEqualTo(ACCEPTED);

        HtmlPage page = webClient.getPage(BASE_URL + "/hash/" + hash);

        DomNodeList<HtmlElement> tables = page.getBody().getElementsByTagName("table");

        assertThat(tables.size()).isEqualTo(1);
        assertThat(((HtmlTable)tables.get(0)).getRows().size()).isEqualTo(3);
    }
}
