package helper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonObjectMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T convert(String json, Class<T> clazz){
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Can not convert convert object");
        }
    }
}
