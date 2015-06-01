package functional.html;

import functional.ApplicationTests;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import play.libs.ws.WSResponse;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;


public class SearchPagesTest extends ApplicationTests {

    @Test
    public void testGetSearchPage() throws Exception {

        WSResponse response = get("/");

        assertThat(response.getStatus()).isEqualTo(OK);
        String body = response.getBody();
        assertThat(body).contains("Search register entries");

        assertThat(body).contains("<a class=\"link_to_register\" href=\"http://localhost:8888/field/test-register\">test-register</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/name\">name</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/key1\">key1</a>, " +
                "<a class=\"link_to_register\" href=\"http://localhost:8888/field/key2\">key2</a>");

    }

    @Test
    public void testSearchReturnsEntriesWithKeyHashNameDisplayed() throws Exception {

        postJson("/create", "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");

        WSResponse response = get("/search?_query=value1");
        assertThat(response.getStatus()).isEqualTo(OK);

        org.jsoup.nodes.Document html = Jsoup.parse(response.getBody());

        Element table = html.getElementById("entries");
        assertThat(table).isNotNull();
        Elements tr = table.select("tr");
        Elements th = tr.first().select("th");
        assertThat(th.get(0).text()).isEqualTo("hash");
        assertThat(th.get(1).select("span.field-value").text()).isEqualTo("test-register");
        assertThat(th.get(2).select("span.field-value").text()).isEqualTo("name");
    }

    
}
