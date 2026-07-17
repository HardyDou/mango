package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateOutputFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 模板渲染命令。
 */
@Data
@Schema(description = "模板渲染命令")
public class TemplateRenderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 128, message = "模板编码不能超过128个字符")
    @Schema(description = "模板编码。业务侧唯一调用键")
    private String templateCode;

    @Deprecated
    @Size(max = 128, message = "业务KEY不能超过128个字符")
    @Schema(description = "业务KEY。兼容历史调用，新调用统一使用模板编码")
    private String businessKey;

    @Positive(message = "模板版本号必须为正数")
    @Schema(description = "模板版本号，不传时使用当前发布版本")
    private Integer versionNo;

    @NotNull(message = "输出格式不能为空")
    @Schema(description = "输出格式：TEXT、HTML、DOCX、XLSX、PDF、OFD")
    private TemplateOutputFormat outputFormat;

    @Valid
    @NotNull(message = "模板变量不能为空")
    @Schema(description = "业务调用方在渲染时传入的动态变量数据")
    private TemplateJsonRequest variables = TemplateJsonRequest.of(null);

    @NotNull(message = "是否异步处理不能为空")
    @Schema(description = "是否异步处理")
    private Boolean async = false;

    @Size(max = 64, message = "业务类型不能超过64个字符")
    @Schema(description = "业务类型")
    private String bizType;

    @Size(max = 128, message = "业务ID不能超过128个字符")
    @Schema(description = "业务ID")
    private String bizId;
}
