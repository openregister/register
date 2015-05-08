package uk.gov.openregister;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class JsonObjectMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T convert(String json, Class<T> clazz){
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Can not convert object");
        }
    }

    public static String convertToString(Map<String, Object> json) {
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not convert object to string");
        }
    }
}
