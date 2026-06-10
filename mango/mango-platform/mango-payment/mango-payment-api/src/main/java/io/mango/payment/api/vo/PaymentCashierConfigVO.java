package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收银台配置视图")
public class PaymentCashierConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收银台配置 ID")
    private Long id;

    @Schema(description = "收银台名称")
    private String cashierName;

    @Schema(description = "适用应用 ID")
    private Long applicationId;

    @Schema(description = "适用应用名称")
    private String applicationName;

    @Schema(description = "是否默认收银台：1-是，0-否")
    private Integer defaultCashier;

    @Schema(description = "允许企业主体 ID，逗号分隔")
    private String enterpriseSubjectIds;

    @Schema(description = "允许企业主体名称，逗号分隔")
    private String enterpriseSubjectNames;

    @Schema(description = "可见标准支付方式编码，逗号分隔")
    private String methodCodes;

    @Schema(description = "可见支付方式名称，逗号分隔")
    private String methodNames;

    @Schema(description = "默认标准支付方式编码")
    private String defaultMethodCode;

    @Schema(description = "默认支付方式名称")
    private String defaultMethodName;

    @Schema(description = "结果跳转地址")
    private String resultReturnUrl;

    @Schema(description = "基础展示主体配置 JSON，包括 logoFileId、subtitle、helpText")
    private String displayConfig;

    @Schema(description = "收银台访问路径")
    private String cashierPath;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
