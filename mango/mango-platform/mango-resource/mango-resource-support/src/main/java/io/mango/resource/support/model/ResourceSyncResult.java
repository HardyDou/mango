package io.mango.resource.support.model;

import io.mango.resource.api.enums.ResourceSyncDisposition;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 目标模块同步结果。
 */
@Value
public class ResourceSyncResult {

    Long targetId;
    String targetTable;
    String message;
    ResourceSyncDisposition disposition;
    LocalDateTime synchronizationTime;

    /**
     * Backward-compatible applied result for handlers that do not yet coordinate target timestamps.
     */
    public static ResourceSyncResult of(Long targetId, String targetTable, String message) {
        return new ResourceSyncResult(
                targetId, targetTable, message, ResourceSyncDisposition.APPLIED, null);
    }

    public static ResourceSyncResult applied(Long targetId, String targetTable, String message,
                                             LocalDateTime synchronizationTime) {
        return new ResourceSyncResult(
                targetId, targetTable, message, ResourceSyncDisposition.APPLIED, synchronizationTime);
    }

    public static ResourceSyncResult preserved(Long targetId, String targetTable, String message) {
        return new ResourceSyncResult(
                targetId, targetTable, message, ResourceSyncDisposition.PRESERVED, null);
    }

    public static ResourceSyncResult skipped(Long targetId, String targetTable, String message) {
        return new ResourceSyncResult(
                targetId, targetTable, message, ResourceSyncDisposition.SKIPPED, null);
    }
}
