package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateOutputFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模板渲染命令。
 */
@Data
@Schema(description = "模板渲染命令")
public class TemplateRenderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板编码。业务侧唯一调用键")
    private String templateCode;

    @Deprecated
    @Schema(description = "业务KEY。兼容历史调用，新调用统一使用模板编码")
    private String businessKey;

    @Schema(description = "模板版本号，不传时使用当前发布版本")
    private Integer versionNo;

    @NotNull
    @Schema(description = "输出格式：TEXT、HTML、DOCX、XLSX、PDF、OFD")
    private TemplateOutputFormat outputFormat;

    @Schema(description = "业务调用方在渲染时传入的动态变量数据")
    private Map<String, Object> variables = new LinkedHashMap<>();

    @Schema(description = "是否异步处理")
    private Boolean async = false;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;
}
