package io.mango.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson utility class for JSON serialization.
 *
 * @author Mango
 * @deprecated Use {@link io.mango.infra.web.util.JacksonUtils} instead.
 *             This class remains here for backward compatibility.
 *             Will be removed in a future version.
 */
@Deprecated
public class JacksonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    private JacksonUtils() {
    }

    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
