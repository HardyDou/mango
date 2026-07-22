package io.mango.resource.core.sync;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Resource registry business-key lookup tuple.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class ResourceRegistryLookupKey {

    private final String resourceType;
    private final String bizKey;
}
