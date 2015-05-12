package functional;

import controllers.App;
import uk.gov.openregister.config.Register;
import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class RegisterInitTest extends ApplicationTests {

    @Test
    public void testKeysAreReadOnStartup() throws Exception {
        assertThat(App.instance.register.fieldNames()).isEqualTo(Arrays.asList("test-register", "name", "key1", "key2"));
    }

    @Test
    public void testFriendlyNameIsReadOnStartup() throws Exception {
        assertThat(App.instance.register.friendlyName()).isEqualTo("Test Register");
    }
}
