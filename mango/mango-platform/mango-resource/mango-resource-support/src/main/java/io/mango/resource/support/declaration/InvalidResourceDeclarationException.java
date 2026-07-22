package io.mango.resource.support.declaration;

/**
 * Deterministic declaration file or schema failure that requires the declaration source to change.
 */
public class InvalidResourceDeclarationException extends IllegalStateException {

    public InvalidResourceDeclarationException(String message) {
        super(message);
    }

    public InvalidResourceDeclarationException(String message, Throwable cause) {
        super(message, cause);
    }
}
