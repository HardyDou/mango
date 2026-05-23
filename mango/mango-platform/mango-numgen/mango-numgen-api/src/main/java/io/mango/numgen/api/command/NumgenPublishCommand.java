package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "规则发布命令")
public class NumgenPublishCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则 ID。按指定规则发布时传入")
    private Long ruleId;

    @Size(max = 128, message = "业务 Key 不能超过128个字符")
    @Schema(description = "业务 Key。未传规则 ID 时，发布该业务 Key 下最新草稿版本")
    private String genKey;
}
