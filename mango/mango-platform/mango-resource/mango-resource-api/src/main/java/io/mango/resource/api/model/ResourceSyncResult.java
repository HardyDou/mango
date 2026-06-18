package io.mango.resource.api.model;

import lombok.Value;

/**
 * 目标模块同步结果。
 */
@Value(staticConstructor = "of")
public class ResourceSyncResult {

    Long targetId;
    String targetTable;
    String message;
}
