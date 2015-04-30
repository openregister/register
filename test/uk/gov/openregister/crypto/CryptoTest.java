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

package uk.gov.openregister.crypto;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CryptoTest {

    @Test
    public void testHashFunction() throws Exception {
        assertThat(Digest.shasum("{}")).isEqualTo("9e26dfeeb6e641a33dae4961196235bdb965b21b");
        assertThat(Digest.shasum("{\"foo\":\"Foo Value\"}")).isEqualTo("257b86bf0b88dbf40cacff2b649f763d585df662");
        assertThat(Digest.shasum("{\"bar\":\"こんにちは、元気ですか\",\"foo\":\"Foo Value\"}")).isEqualTo("d8d2a8d65415145e4ca092af80cc4c6bfa34519c");
    }
}
