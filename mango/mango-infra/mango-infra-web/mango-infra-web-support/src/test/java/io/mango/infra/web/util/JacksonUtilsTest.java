package io.mango.infra.web.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonUtilsTest {

    @Test
    void toJsonStr_serializesLongAndJavaTimeUsingWebContract() {
        WebValue value = new WebValue(9_007_199_254_740_993L,
                LocalDateTime.of(2026, 7, 15, 9, 8, 7),
                LocalDate.of(2026, 7, 15),
                LocalTime.of(9, 8, 7),
                Instant.parse("2026-07-15T01:08:07Z"));

        assertEquals("{\"id\":\"9007199254740993\",\"dateTime\":\"2026-07-15 09:08:07\","
                        + "\"date\":\"2026-07-15\",\"time\":\"09:08:07\","
                        + "\"instant\":\"2026-07-15T01:08:07Z\"}",
                JacksonUtils.toJsonStr(value));
    }

    @Test
    void convertValue_nullOrAlreadyTyped_preservesExistingContract() {
        WebValue value = new WebValue(1L, null, null, null, null);

        assertNull(JacksonUtils.toJsonStr(null));
        assertNull(JacksonUtils.convertValue(value, null));
        assertNull(JacksonUtils.convertValue(null, WebValue.class));
        assertSame(value, JacksonUtils.convertValue(value, WebValue.class));
    }

    @Test
    void convertValue_convertsCompatibleMapAndRejectsIncompatibleValue() {
        assertEquals(new IdValue(12L), JacksonUtils.convertValue(java.util.Map.of("id", 12L), IdValue.class));
        assertThrows(IllegalArgumentException.class, () -> JacksonUtils.convertValue("bad", IdValue.class));
    }

    record WebValue(Long id, LocalDateTime dateTime, LocalDate date, LocalTime time, Instant instant) {
    }

    record IdValue(Long id) {
    }
}
