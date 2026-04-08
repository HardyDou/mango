package io.mango.dal.api;

/**
 * Serializer interface for JSON/Protobuf serialization.
 */
public interface ISerializer {

    /**
     * Serialize object to string.
     * @param object object to serialize (must not be null)
     * @return serialized string
     */
    String serialize(Object object);

    /**
     * Deserialize string to object.
     * @param content  serialized content (must not be null or blank)
     * @param classType target class type
     * @param <T>       target type
     * @return deserialized object
     */
    <T> T deserialize(String content, Class<T> classType);
}