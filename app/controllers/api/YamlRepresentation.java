package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlRepresentation extends JacksonRepresentation {
    public YamlRepresentation() {
        super(new ObjectMapper(new YAMLFactory()), "text/yaml; charset=utf-8");
    }

    public static Representation instance = new YamlRepresentation();

}
