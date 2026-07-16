package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源批量同步结果。
 */
@Data
@Schema(description = "资源批量同步结果")
public class ResourceBatchResultVO {

    @Schema(description = "逐资源同步结果")
    private List<ResourceBatchEntryVO> entries = new ArrayList<>();

    public List<ResourceBatchEntryVO> getEntries() {
        return List.copyOf(entries);
    }

    public void setEntries(List<ResourceBatchEntryVO> entries) {
        if (entries == null) {
            this.entries = new ArrayList<>();
            return;
        }
        this.entries = new ArrayList<>(entries);
    }
}
