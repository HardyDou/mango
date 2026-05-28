package io.mango.infra.sensitive.api;

import java.util.List;

/**
 * Provides sensitive word allow and deny lists for the houbb sensitive-word engine.
 */
public interface ISensitiveWordProvider {

    /**
     * Words that should be allowed even when matched by a deny source.
     *
     * @return allow list
     */
    default List<String> allowWords() {
        return List.of();
    }

    /**
     * Words that should be denied.
     *
     * @return deny list
     */
    default List<String> denyWords() {
        return List.of();
    }
}
