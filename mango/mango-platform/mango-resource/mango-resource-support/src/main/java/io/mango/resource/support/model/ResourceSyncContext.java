package io.mango.resource.support.model;

import lombok.Value;

import java.time.LocalDateTime;

/**
 * Immutable context for one Resource target synchronization attempt.
 */
@Value(staticConstructor = "of")
public class ResourceSyncContext {

    String resourceId;
    LocalDateTime previousSyncTime;
    LocalDateTime synchronizationTime;
    Long targetId;
    String targetTable;
}
