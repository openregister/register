package uk.gov.openregister;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class JsonObjectMapper {
    private static final ObjectMapper objectMapper = objectMapper(null);

    public static ObjectMapper objectMapper(JsonFactory jsonFactory){
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS , false);
        objectMapper.setDateFormat(new SimpleDateFormat("dd MMMM yyyy HH:mm:ss z"));
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return objectMapper;
    }

    public static <T> T convert(JsonNode jsonNode, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Can not convert object");
        }
    }

    public static String convertToString(Object obj){
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not convert object to string");
        }

    }
}
