package io.mango.dal.api;

/**
 * Object converter / mapper interface.
 */
public interface IConverter {

    /**
     * Convert source object to target type.
     * @param source   source object (must not be null)
     * @param classType target class type
     * @param <T>      target type
     * @return converted object
     */
    <T> T convert(Object source, Class<T> classType);
}