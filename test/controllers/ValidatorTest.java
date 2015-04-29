package controllers;

import org.junit.Test;
import play.libs.Json;
import uk.gov.openregister.domain.Record;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class ValidatorTest {

    @Test
    public void testValidRecord() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"]}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(true);
        assertThat(result.getMessages()[0]).isEqualTo("Valid Record");
    }

    @Test
    public void testFailWhenRecordIsEmpty() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"\",\"key2\":[\"entry1\",\"entry2\"]}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are mandatory but not found in record: key1");
    }

    @Test
    public void testRecordWithMissingMandatoryField() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are mandatory but not found in record: key2");
    }

    @Test
    public void testRecordWithMissingMandatoryFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are mandatory but not found in record: key1, key2");
    }

    @Test
    public void testRecordWithExtraField() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"],\"key3\":\"valuey\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are allowed in the record: key3");
    }

    @Test
    public void testRecordWithExtraFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key2\":[\"entry1\",\"entry2\"],\"key3\":\"valuey\",\"key4\":\"valuez\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are allowed in the record: key3, key4");
    }

    @Test
    public void testRecordWithMissingAndExtraFields() throws Exception {

        Validator v = new Validator(Arrays.asList("key1", "key2"));
        String json = "{\"key1\":\"valuex\",\"key3\":\"valuey\",\"key4\":\"valuez\"}";

        final ValidationResult result = v.validate(new Record(Json.parse(json)));
        assertThat(result.isValid()).isEqualTo(false);
        assertThat(result.getMessages()[0]).isEqualTo("The following keys are allowed in the record: key3, key4");
        assertThat(result.getMessages()[1]).isEqualTo("The following keys are mandatory but not found in record: key2");
    }

}
