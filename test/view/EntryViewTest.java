/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package view;

import org.jsoup.Jsoup;
import org.junit.Test;
import play.twirl.api.Html;
import uk.gov.openregister.domain.Record;
import views.html.entry;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;

public class EntryViewTest {
    @Test
    public void testEntryDoesntShowsNameIfNotPresent() throws Exception {
        String json = "{\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";

        Html render = entry.render("theregister", Arrays.asList("key1", "key2"), new Record(json));
        org.jsoup.nodes.Document html = Jsoup.parse(contentAsString(render));

        assertThat(html.getElementById("entry_name")).isNull();
    }
}
