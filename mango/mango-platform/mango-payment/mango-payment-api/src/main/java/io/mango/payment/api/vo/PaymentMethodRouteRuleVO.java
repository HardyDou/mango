package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "支付方式路由规则视图")
public class PaymentMethodRouteRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由规则 ID")
    private Long id;

    @Schema(description = "路由规则编码")
    private String ruleCode;

    @Schema(description = "路由规则名称")
    private String ruleName;

    @Schema(description = "应用 ID")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "标准支付方式名称")
    private String methodName;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "内部路由域")
    private String environment;

    @Schema(description = "路由模式")
    private String routeMode;

    @Schema(description = "是否允许失败降级")
    private Integer fallbackEnabled;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "路由明细")
    private List<PaymentMethodRouteRuleItemVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
