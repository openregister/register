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

package uk.gov.openregister.domain;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RecordTest {


    @Test
    public void testHash() throws Exception {

        assertThat(new Record("{\"foo\":\"Foo Value\"}")
                .getHash()).isEqualTo("257b86bf0b88dbf40cacff2b649f763d585df662");

    }

    @Test
    public void testKeysAreSortedBeforeSaving() throws Exception {

        assertThat(new Record("{\"b\":\"value\",\"a\":\"another\"}").normalise())
                .isEqualTo("{\"a\":\"another\",\"b\":\"value\"}");

    }

    @Test
    public void testWhitespacesAreRemovedBeforeSaving() throws Exception {

        assertThat(new Record("{\"a\": \"value\", \n\"b\": \"another\"}").normalise())
                .isEqualTo("{\"a\":\"value\",\"b\":\"another\"}");

    }
}
