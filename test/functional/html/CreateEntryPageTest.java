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

package functional.html;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import functional.ApplicationTests;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreateEntryPageTest extends ApplicationTests {

    @Test
    public void addNewEntry_ignoresTheExtraFormFieldsAndCreatesANewEntryInTheRegister() throws IOException {
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
}
