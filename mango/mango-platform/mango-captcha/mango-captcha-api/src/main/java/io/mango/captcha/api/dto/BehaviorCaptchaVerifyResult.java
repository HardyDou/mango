package io.mango.captcha.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 无感行为验证评分结果。
 */
@Data
@Schema(description = "无感行为验证评分结果")
public class BehaviorCaptchaVerifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码键")
    private String key;

    @Schema(description = "0.0 到 1.0 的行为评分，分数越高越像真人")
    private double score;

    @Schema(description = "是否通过当前验证码校验")
    private boolean passed;

    @Schema(description = "风险等级：LOW/MEDIUM/HIGH")
    private String riskLevel;

    @Schema(description = "建议业务动作：ALLOW/SECONDARY_VERIFY/DENY")
    private String suggestAction;

    @Schema(description = "评分原因，多个原因用逗号分隔")
    private String reason;
}
