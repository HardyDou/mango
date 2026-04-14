package io.mango.infra.web.api;

import java.util.List;

/**
 * Internal path provider abstraction.
 * <p>
 * Infra-web depends on this interface to get internal path patterns,
 * without coupling to business platform (e.g., mango-rbac).
 * </p>
 * <p>
 * Implementations should be provided by business modules (e.g., mango-rbac).
 * </p>
 *
 * @author Mango
 */
public interface IInternalPathProvider {

    /**
     * Get all internal path patterns
     *
     * @return list of internal path patterns (e.g., "/admin/**", "/internal/*")
     */
    List<String> getInternalPaths();
}
