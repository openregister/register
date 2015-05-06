package uk.gov.openregister.validation;

import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidatorTest {

    @Test
    public void validate_returnsEmptyListWhenNoValidationError() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}";

        List<ValError> result = v.validate(new Record(Json.parse(json)));
        assertTrue(result.isEmpty());
    }

    @Test
    public void validate_returnsListOfAllErrors() throws Exception {

        Validator v = new Validator(Arrays.asList("name","key1", "key2"));
        String json = "{\"invalidKey\":\"invalidKeyValue\",\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}";

        List<ValError> result = v.validate(new Record(Json.parse(json)));
        assertEquals(2, result.size());

        ValError error1 = result.get(0);
        assertEquals("invalidKey", error1.key);
        assertEquals("Key not required", error1.message);

        ValError error2 = result.get(1);
        assertEquals("name", error2.key);
        assertEquals("Missing required key", error2.message);
    }
}
