package io.mango.infra.tools.doc;

/**
 * Exception thrown by local document tool implementations.
 */
public class DocumentToolException extends RuntimeException {

    public DocumentToolException(String message) {
        super(message);
    }

    public DocumentToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
