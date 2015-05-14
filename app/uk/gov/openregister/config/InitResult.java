package uk.gov.openregister.config;

import java.util.ArrayList;
import java.util.List;

public class InitResult {

    public static InitResult OK = new InitResult(true);

    public InitResult(boolean started) {
        this.started = started;
    }

    boolean started = false;
    List<String> errors = new ArrayList<>();

    public boolean isStarted() {
        return started;
    }

    public List<String> errors() {
        return errors;
    }
}
