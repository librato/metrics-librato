package com.librato.metrics.reporter;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class Json {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T decode(String json, Class<T> klazz) {
        try {
            return mapper.readValue(json, klazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
