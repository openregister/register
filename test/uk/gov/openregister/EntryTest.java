package uk.gov.openregister;

import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.domain.Entry;

public class EntryTest {


    @Test
    public void testHash() throws Exception {

        assertThat(new Entry(Json.parse("{\"foo\":\"Foo Value\"}"))
                .getHash()).isEqualTo("257b86bf0b88dbf40cacff2b649f763d585df662");

    }

}
