package io.mango.infra.web.support;

/**
 * Servlet request attributes produced after an internal call has been verified.
 */
public final class InternalCallAttributes {

    public static final String VERIFIED = InternalCallAttributes.class.getName() + ".VERIFIED";

    private InternalCallAttributes() {
    }
}
