package controllers.html;

import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSResponse;
import uk.gov.openregister.ApplicationTests;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class SearchPagesTest extends ApplicationTests {

    @Test
    public void testGetSearchPage() throws Exception {

        WSResponse response = get("/");

        assertThat(response.getStatus()).isEqualTo(OK);
        String body = response.getBody();
        assertThat(body).contains("The official register of test entries throughout the UK");
        assertThat(body).contains("Part of");
        assertThat(body).contains("test registry");
        assertThat(body).contains("This register exists as an accurate list of all test entries");
        assertThat(body).contains("Search this register");
    }


    @Test
    public void testGetSearchPageShowsTheTotalAmountOfEntries() throws Exception {
        Document r1 = Document.parse(new Record(Json.parse("{\"key\":\"value1\"}")).toString());
        Document r2 = Document.parse(new Record(Json.parse("{\"key\":\"value2\"}")).toString());
        Document r3 = Document.parse(new Record(Json.parse("{\"key\":\"value1\",\"another\":\"value\"}")).toString());

        collection().insertMany(Arrays.asList(r1, r2, r3));

        WSResponse response = get("/");

        assertThat(response.getStatus()).isEqualTo(OK);
        String body = response.getBody();
        assertThat(body).contains("<span class=\"big-number\">3</span>");
    }

    @Test
    public void testSubmitSearchQueryAndReturnsListOfEntries() throws Exception {
        Document r1 = Document.parse(new Record(Json.parse("{\"key\":\"value1\",\"another\":\"1\"}")).toString());
        Document r2 = Document.parse(new Record(Json.parse("{\"key\":\"value2\",\"another\":\"2\"}")).toString());
        Document r3 = Document.parse(new Record(Json.parse("{\"key\":\"value1\",\"another\":\"3\"}")).toString());

        collection().insertMany(Arrays.asList(r1, r2, r3));

        WSResponse response = get("/search?_query=value1");
        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element table = html.getElementById("entries-table");
        assertThat(table).isNotNull();
        Elements tr = table.select("tr");
        Elements th = tr.first().select("th");
        assertThat(th.get(0).text()).isEqualTo("hash");
        assertThat(th.get(1).text()).isEqualTo("key");
        assertThat(th.get(2).text()).isEqualTo("another");

        Elements td1 = tr.get(1).select("td");
        assertThat(td1.get(0).select("a").first().toString()).isEqualTo("<a href=\"/hash/9c2d20f19cc9e3f19e555f4d4d2794b9314883fe\">9c2d20f</a>");
        assertThat(td1.get(1).text()).isEqualTo("value1");
        assertThat(td1.get(2).text()).isEqualTo("1");

        Elements td2 = tr.get(2).select("td");
        assertThat(td2.get(0).select("a").first().toString()).isEqualTo("<a href=\"/hash/7eef71cc8b5f3be28b7920456ee78d2f79afa76f\">7eef71c</a>");
        assertThat(td2.get(1).text()).isEqualTo("value1");
        assertThat(td2.get(2).text()).isEqualTo("3");
    }

    @Test
    public void testRenderFieldWithoutKeyValueAndAFieldWithValueIsAList() throws Exception {
        Record record1 = new Record(Json.parse("{\"key\":\"value1\",\"another\":\"1\",\"listFields\":[\"A\",\"B\"]}"));
        Record record2 = new Record(Json.parse("{\"key\":\"value1\",\"another\":\"1\",\"listFields\":[\"A\",\"B\"], \"key1\":\"someValue\"}"));

        collection().insertOne(Document.parse(record2.toString()));
        collection().insertOne(Document.parse(record1.toString()));

        WSResponse response = get("/hash/" + record1.getHash());

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element dl = html.select("dl").first();
        assertThat(dl).isNotNull();

        Elements dt = dl.select("dt");
        Elements dd = dl.select("dd");

        assertThat(dt.get(0).text()).isEqualTo("hash");
        assertThat(dd.get(0).text()).isEqualTo(record1.getHash());

        assertThat(dt.get(1).text()).isEqualTo("key");
        assertThat(dd.get(1).text()).isEqualTo("value1");


        assertThat(dt.get(2).text()).isEqualTo("another");
        assertThat(dd.get(2).text()).isEqualTo("1");

        assertThat(dt.get(3).text()).isEqualTo("listFields");
        assertThat(dd.get(3).text()).isEqualTo("['A', 'B']");

        assertThat(dt.get(4).text()).isEqualTo("key1");
        assertThat(dd.get(4).text()).isEqualTo("");
    }

    @Test
    public void testEntryShowsNameIfPresent() throws Exception {
        Record record = new Record(Json.parse("{\"key\":\"value1\",\"name\":\"The Entry\",\"another\":\"1\"}"));
        Document r1 = Document.parse(record.toString());

        collection().insertOne(r1);

        WSResponse response = get("/hash/" + record.getHash());

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element h1 = html.getElementById("entry_name");
        assertThat(h1).isNotNull();
        assertThat(h1.text()).isEqualTo("The Entry");
    }


    @Test
    public void testEntryDoesntShowsNameIfNotPresent() throws Exception {
        Record record = new Record(Json.parse("{\"key\":\"value1\",\"another\":\"1\"}"));
        Document r1 = Document.parse(record.toString());

        collection().insertOne(r1);

        WSResponse response = get("/hash/" + record.getHash());

        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        assertThat(html.getElementById("entry_name")).isNull();
    }
}
