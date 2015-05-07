package functional;

import controllers.conf.Register;
import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class RegisterInitTest extends ApplicationTests {

    @Test
    public void testKeysAreReadOnStartup() throws Exception {
        assertThat(Register.instance.registerInfo().keys).isEqualTo(Arrays.asList("test-register", "name", "key1", "key2"));
    }

    @Test
    public void testFriendlyNameIsReadOnStartup() throws Exception {
        assertThat(Register.instance.friendlyName()).isEqualTo("Test Register");
    }
}
