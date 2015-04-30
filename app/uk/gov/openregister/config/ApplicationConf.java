package uk.gov.openregister.config;

import play.Play;

public class ApplicationConf {


    public static String getString(String key) {
        return Play.application().configuration().getString(key);
    }
}
