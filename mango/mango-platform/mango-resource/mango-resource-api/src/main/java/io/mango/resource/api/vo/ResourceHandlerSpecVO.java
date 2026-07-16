package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源处理器字段契约。
 */
@Data
@Schema(description = "资源处理器字段契约")
public class ResourceHandlerSpecVO {

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "必填字段")
    private List<String> requiredFields = new ArrayList<>();

    @Schema(description = "字段中文说明")
    private List<ResourceHandlerFieldVO> fields = new ArrayList<>();

    public List<String> getRequiredFields() {
        return List.copyOf(requiredFields);
    }

    public void setRequiredFields(List<String> requiredFields) {
        if (requiredFields == null) {
            this.requiredFields = new ArrayList<>();
            return;
        }
        this.requiredFields = new ArrayList<>(requiredFields);
    }

    public List<ResourceHandlerFieldVO> getFields() {
        return List.copyOf(fields);
    }

    public void setFields(List<ResourceHandlerFieldVO> fields) {
        if (fields == null) {
            this.fields = new ArrayList<>();
            return;
        }
        this.fields = new ArrayList<>(fields);
    }
}
