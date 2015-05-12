package view;

import org.jsoup.Jsoup;
import org.junit.Test;
import play.twirl.api.Html;
import uk.gov.openregister.domain.RecordVersionInfo;
import uk.gov.openregister.domain.Record;
import views.html.entry;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;

public class EntryViewTest {
    @Test
    public void testEntryDoesntShowsNameIfNotPresent() throws Exception {
        String json = "{\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";

        Html render = entry.render(Arrays.asList("key1", "key2"), new Record(json), Collections.<RecordVersionInfo>emptyList());
        org.jsoup.nodes.Document html = Jsoup.parse(contentAsString(render));

        assertThat(html.getElementById("entry_name")).isNull();
    }
}
