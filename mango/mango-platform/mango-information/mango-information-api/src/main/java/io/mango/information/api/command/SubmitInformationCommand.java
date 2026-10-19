package io.mango.information.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 公开或服务端信息采集提交命令。 */
@Getter
@Setter
@ToString
public class SubmitInformationCommand implements Serializable {
    @NotBlank(message = "表单编码不能为空")
    @Size(max = 64, message = "表单编码不能超过64个字符")
    private String formCode;

    @NotNull(message = "提交内容不能为空")
    @Schema(description = "字段编码到字段值的映射")
    private Map<String, Object> values;

    @Size(max = 500, message = "来源页面不能超过500个字符")
    private String sourcePage;

    @Size(max = 128, message = "幂等键不能超过128个字符")
    private String idempotencyKey;

    @Schema(description = "服务端 API 专属 SK，仅服务端接入时传入")
    private String serverSecret;

    /**
     * 防御性拷贝：值表不向调用方暴露内部实例，写入时同样复制一份，
     * 避免外部持有引用后修改影响校验与落库结果。
     */
    public Map<String, Object> getValues() {
        return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? null : new LinkedHashMap<>(values);
    }
}
