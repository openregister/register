package uk.gov.openregister;

import org.junit.Test;
import uk.gov.openregister.crypto.Digest;

import static org.fest.assertions.Assertions.assertThat;

public class CryptoTest {

    @Test
    public void testHashFunction() throws Exception {
        assertThat(Digest.shasum("{}")).isEqualTo("9e26dfeeb6e641a33dae4961196235bdb965b21b");
        assertThat(Digest.shasum("{\"foo\":\"Foo Value\"}")).isEqualTo("257b86bf0b88dbf40cacff2b649f763d585df662");
        assertThat(Digest.shasum("{\"bar\":\"こんにちは、元気ですか\",\"foo\":\"Foo Value\"}")).isEqualTo("d8d2a8d65415145e4ca092af80cc4c6bfa34519c");
    }
}
