package controllers;

import uk.gov.openregister.config.*;

import java.util.List;
import java.util.Optional;

public class App {

    public static App instance = new App();

    public Register register;

    private InitResult initResult;

    public void init() {

        String name = Optional.ofNullable(ApplicationConf.getString("register.name")).orElse("unknown-register");

        if ("register".equalsIgnoreCase(name)) {
            register = new RegisterRegister();
        } else if ("field".equalsIgnoreCase(name)) {
            register = new FieldRegister();
        } else if ("datatype".equalsIgnoreCase(name)) {
            register = new DatatypeRegister();
        } else {
            register = new GenericRegister(name);
        }

        initResult = register.init();
    }

    public boolean started() {
        return initResult.isStarted();
    }

    public List<String> getInitErrors() {
        return initResult.errors();
    }

}
