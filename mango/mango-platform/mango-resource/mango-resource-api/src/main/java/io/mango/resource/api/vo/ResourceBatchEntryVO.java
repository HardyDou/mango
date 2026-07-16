package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单个资源批量同步结果。
 */
@Data
@Schema(description = "单个资源批量同步结果")
public class ResourceBatchEntryVO {

    @Schema(description = "稳定资源ID")
    private String resourceId;

    @Schema(description = "同步结果")
    private ResourceSyncResultVO result;

    public ResourceSyncResultVO getResult() {
        return copyResult(result);
    }

    public void setResult(ResourceSyncResultVO result) {
        this.result = copyResult(result);
    }

    private static ResourceSyncResultVO copyResult(ResourceSyncResultVO source) {
        if (source == null) {
            return null;
        }
        ResourceSyncResultVO copy = new ResourceSyncResultVO();
        copy.setTargetId(source.getTargetId());
        copy.setTargetTable(source.getTargetTable());
        copy.setMessage(source.getMessage());
        return copy;
    }
}
