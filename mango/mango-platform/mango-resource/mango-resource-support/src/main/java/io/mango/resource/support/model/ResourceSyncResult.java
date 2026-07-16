package io.mango.resource.support.model;

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
