package uk.gov.openregister.validation;

import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class ValidatorTest {

    @Test
    public void testValidRecord() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(true);
    }

    @Test
    public void testFailWhenRecordIsEmpty() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"\",\"key2\":[\"entry1\",\"entry2\"]}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys()).isEqualTo(Collections.singletonList("key1"));
        assertThat(result.getInvalidKeys().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testRecordWithMissingMandatoryField() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys()).isEqualTo(Collections.singletonList("key2"));
        assertThat(result.getInvalidKeys().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testRecordWithMissingMandatoryFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys()).isEqualTo(Arrays.asList("key1", "key2"));
        assertThat(result.getInvalidKeys().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testRecordWithExtraField() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"],\"key3\":\"valuey\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys().isEmpty()).isEqualTo(true);
        assertThat(result.getInvalidKeys()).isEqualTo(Collections.singletonList("key3"));
    }

    @Test
    public void testRecordWithExtraFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"],\"key3\":\"valuey\",\"key4\":\"valuez\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys().isEmpty()).isEqualTo(true);
        assertThat(result.getInvalidKeys()).isEqualTo(Arrays.asList("key3", "key4"));
    }

    @Test
    public void testRecordWithMissingAndExtraFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key3\":\"valuey\",\"key4\":\"valuez\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));

        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMissingKeys()).isEqualTo(Collections.singletonList("key2"));
        assertThat(result.getInvalidKeys()).isEqualTo(Arrays.asList("key3", "key4"));
    }

}
