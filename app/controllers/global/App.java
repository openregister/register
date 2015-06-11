package controllers.global;

import uk.gov.openregister.config.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class App {

    private static App instance = new App();

    private final Map<String, Register> registers = new ConcurrentHashMap<>();

    public static Register getRegister(String registerName) {
        return instance.__getRegister(registerName);
    }

    public static String registerName(String host) {

        // This is a hack for unit-tests
        if(host.startsWith("localhost")) return "test-register";
        else if(host.endsWith("herokuapp.com")) return host.split("-")[0];
        else return host.replaceAll("([^\\.]+)\\.openregister\\..*", "$1");
    }

    private Register __getRegister(String registerName) {

        if (!registers.containsKey(registerName)) {
            Register register;
            if ("register".equalsIgnoreCase(registerName)) {
                register = new RegisterRegister();
            } else if ("field".equalsIgnoreCase(registerName)) {
                register = new FieldRegister();
            } else if ("datatype".equalsIgnoreCase(registerName)) {
                register = new DatatypeRegister();
            } else {
                register = new GenericRegister(registerName);
            }
            registers.put(registerName, register);
        }

        return registers.get(registerName);
    }
}
