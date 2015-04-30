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

package functional.json;

import org.junit.Test;
import play.libs.ws.WSResponse;
import functional.ApplicationTests;

import static org.fest.assertions.Assertions.assertThat;

public class ApplicationGlobalTest extends ApplicationTests {

    @Test
    public void test404ErrorResponse() throws Exception {
        WSResponse response = postJson("/idonotexist?_representation=json", "{}");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo("{\"status\":404,\"message\":\"Page not found\"}");
    }

    @Test
    public void test400ErrorResponse() throws Exception {
        WSResponse response = postJson("/create?_representation=json", "{");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("{\"status\":400,\"message\":\"Invalid Json\"}");
    }

}
