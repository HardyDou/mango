package io.mango.common.exception;

/**
 * Indicates that an operation can be retried after a referenced dependency becomes available.
 */
public class DependencyNotReadyException extends IllegalStateException {

    public DependencyNotReadyException(String message) {
        super(message);
    }
}
