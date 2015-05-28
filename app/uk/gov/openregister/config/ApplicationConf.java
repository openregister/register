package uk.gov.openregister.config;

import play.Play;

public class ApplicationConf {

    public static String getString(String key) {
        if(Play.application().isDev() && Play.application().configuration().getString("dev." + key) != null)
            return Play.application().configuration().getString("dev." + key);
        else return Play.application().configuration().getString(key);
    }

    public static String registerUrl(String registerName, String path) {
        return Play.application().configuration().getString("registers.service.template.url").replace("__REGISTER__", registerName) + path;
    }

    public static String getRegisterServiceTemplateUrl() {
        return getString("registers.service.template.url");
    }
}
