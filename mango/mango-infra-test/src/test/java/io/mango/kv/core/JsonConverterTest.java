package io.mango.kv.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonConverter.
 */
class JsonConverterTest {

    private final JsonConverter converter = new JsonConverter();

    // ==================== convert() tests ====================

    @Test
    void convert_sameType_returnsEquivalentObject() {
        SourcePerson source = new SourcePerson("John", 30);
        TargetPerson result = converter.convert(source, TargetPerson.class);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    void convert_nullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert(null, TargetPerson.class));
    }

    @Test
    void convert_nullTargetClass_throwsIllegalArgumentException() {
        SourcePerson source = new SourcePerson("John", 30);
        assertThrows(IllegalArgumentException.class, () -> converter.convert(source, null));
    }

    // Helper classes for testing - proper Java beans
    static class SourcePerson {
        private String name;
        private int age;

        SourcePerson() {}

        @JsonCreator
        public SourcePerson(@JsonProperty("name") String name, @JsonProperty("age") int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    static class TargetPerson {
        private String name;
        private int age;

        TargetPerson() {}

        @JsonCreator
        public TargetPerson(@JsonProperty("name") String name, @JsonProperty("age") int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }
}