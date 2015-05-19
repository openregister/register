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
