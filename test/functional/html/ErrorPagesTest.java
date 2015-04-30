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

import org.junit.Test;
import play.libs.ws.WSResponse;
import functional.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;

public class ErrorPagesTest extends ApplicationTests {


    @Test
    public void testUnknownHash() throws Exception {

        WSResponse response = get("/hash/123");

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
        String body = response.getBody();
        assertThat(body).contains("<h1 class=\"error\">Entry not found</h1>");
    }
}
