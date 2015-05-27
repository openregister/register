package uk.gov.openregister.config;

import functional.ApplicationTests;
import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class FieldRegisterTest extends ApplicationTests {

    @Test
    public void testReturnsRegisterNames() throws Exception {
        Record r1 = new Record(Json.parse("{\"register\": \"test-register\",\"name\": \"Test Register\", \"fields\":[\"test-register\", \"name\",\"key1\",\"key2\"]}"));
        Record r2 = new Record(Json.parse("{\"register\": \"test-register-2\",\"name\": \"Test Register 2\", \"fields\":[\"test-register-2\", \"name\",\"key1\"]}"));

        assertThat(new FieldRegister(Arrays.asList(r1, r2)).getRegisterNamesFor("name")).isEqualTo(Arrays.asList("test-register", "test-register-2"));
        assertThat(new FieldRegister(Arrays.asList(r1, r2)).getRegisterNamesFor("key2")).isEqualTo(Collections.singletonList("test-register"));
    }
}
