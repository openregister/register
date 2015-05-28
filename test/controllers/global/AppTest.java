package controllers.global;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class AppTest {
    @Test
    public void register_devEnv_returnsTheRegisterWithTheName() {

        String registerName = App.registerName("abc.openregister.dev");
        assertThat(registerName).isEqualTo("abc");
    }

    @Test
    public void register_returnsTheRegisterWithTheName() {
        String registerName = App.registerName("abc.openregister.org");
        assertThat(registerName).isEqualTo("abc");
    }

    @Test
    public void register_returnsTestRegisterForFunctionalTests() {
        String registerName = App.registerName("localhost:3333");
        assertThat(registerName).isEqualTo("test-register");
    }


}