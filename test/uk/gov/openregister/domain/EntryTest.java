package uk.gov.openregister.domain;

import org.junit.Test;
import play.libs.Json;

import static org.fest.assertions.Assertions.assertThat;

public class EntryTest {


    @Test
    public void testHash() throws Exception {

        assertThat(new RegisterRow(Json.parse("{\"foo\":\"Foo Value\"}"))
                .getHash()).isEqualTo("257b86bf0b88dbf40cacff2b649f763d585df662");

    }

    @Test
    public void testKeysAreSortedBeforeSaving() throws Exception {

        assertThat(new RegisterRow(Json.parse("{\"b\":\"value\",\"a\":\"another\"}")).normalise())
                .isEqualTo("{\"a\":\"another\",\"b\":\"value\"}");

    }

    @Test
    public void testWhitespacesAreRemovedBeforeSaving() throws Exception {

        assertThat(new RegisterRow(Json.parse("{\"a\": \"value\", \n\"b\": \"another\"}")).normalise())
                .isEqualTo("{\"a\":\"value\",\"b\":\"another\"}");

    }
}
