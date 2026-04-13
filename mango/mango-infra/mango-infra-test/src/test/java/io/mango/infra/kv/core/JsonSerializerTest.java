package io.mango.infra.kv.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonSerializer.
 */
class JsonSerializerTest {

    private final JsonSerializer serializer = new JsonSerializer();

    // ==================== serialize() tests ====================

    @Test
    void serialize_string_returnsJsonString() {
        String result = serializer.serialize("hello");
        assertEquals("\"hello\"", result);
    }

    @Test
    void serialize_number_returnsJsonNumber() {
        String result = serializer.serialize(123);
        assertEquals("123", result);
    }

    @Test
    void serialize_boolean_returnsJsonBoolean() {
        assertEquals("true", serializer.serialize(true));
        assertEquals("false", serializer.serialize(false));
    }

    @Test
    void serialize_list_returnsJsonArray() {
        String result = serializer.serialize(List.of(1, 2, 3));
        assertEquals("[1,2,3]", result);
    }

    @Test
    void serialize_map_returnsJsonObject() {
        Map<String, Object> map = Map.of("name", "John", "age", 30);
        String result = serializer.serialize(map);
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"John\""));
        assertTrue(result.contains("\"age\""));
        assertTrue(result.contains("30"));
    }

    @Test
    void serialize_null_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(null));
    }

    // ==================== deserialize() tests ====================

    @Test
    void deserialize_string_returnsString() {
        String result = serializer.deserialize("\"hello\"", String.class);
        assertEquals("hello", result);
    }

    @Test
    void deserialize_integer_returnsInteger() {
        Integer result = serializer.deserialize("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    void deserialize_boolean_returnsBoolean() {
        assertTrue(serializer.deserialize("true", Boolean.class));
        assertFalse(serializer.deserialize("false", Boolean.class));
    }

    @Test
    void deserialize_list_returnsList() {
        String json = "[1,2,3]";
        List<Integer> result = serializer.deserialize(json,
                (Class<List<Integer>>)(Class<?>) List.class);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    void deserialize_object_returnsObject() {
        String json = "{\"name\":\"John\",\"age\":30}";
        TestPerson person = serializer.deserialize(json, TestPerson.class);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    void deserialize_nullContent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(null, String.class));
    }

    @Test
    void deserialize_blankContent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("  ", String.class));
    }

    @Test
    void deserialize_nullClassType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("\"hello\"", null));
    }

    @Test
    void deserialize_invalidJson_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> serializer.deserialize("invalid", String.class));
    }

    // ==================== roundtrip tests ====================

    @Test
    void serialize_deserialize_roundtrip_string() {
        String original = "hello world";
        String json = serializer.serialize(original);
        String result = serializer.deserialize(json, String.class);
        assertEquals(original, result);
    }

    @Test
    void serialize_deserialize_roundtrip_object() {
        TestPerson original = new TestPerson("John", 30);
        String json = serializer.serialize(original);
        TestPerson result = serializer.deserialize(json, TestPerson.class);
        assertEquals(original.getName(), result.getName());
        assertEquals(original.getAge(), result.getAge());
    }

    // Helper class for testing - proper Java bean
    static class TestPerson {
        private String name;
        private int age;

        TestPerson() {}

        @JsonCreator
        public TestPerson(@JsonProperty("name") String name, @JsonProperty("age") int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }
}