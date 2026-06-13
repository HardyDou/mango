package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "收银台可用支付方式视图")
public class PaymentCashierMethodVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付方式名称")
    private String methodName;

    @Schema(description = "收银台展示分组编码，不作为标准支付方式三级分类配置依据")
    private String categoryCode;

    @Schema(description = "收银台分组名称")
    private String categoryName;

    @Schema(description = "收银台分组排序")
    private Integer categorySort;

    @Schema(description = "一级分类：CORPORATE/PERSONAL")
    private String accountNature;

    @Schema(description = "二级分类：WECHAT/ALIPAY/UNIONPAY/BANK_CARD/WALLET/EBANK/OFFLINE_TRANSFER")
    private String instrumentType;

    @Schema(description = "三级分类：QR_CODE/H5_REDIRECT/DEBIT_QUICK/CREDIT_QUICK/WALLET_QUICK/BANK_GATEWAY/ACCOUNT_TRANSFER")
    private String interactionType;

    @Schema(description = "支付物料类型：QR/HTML_FORM/TRANSFER_ACCOUNT/H5_PARAM")
    private String paymentMaterialType;

    @Schema(description = "图标文件 ID")
    private Long iconFileId;

    @Schema(description = "收银台说明")
    private String description;

    @Schema(description = "支付通道 ID")
    private Long channelId;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "支付通道名称")
    private String channelName;

    @Schema(description = "签约 ID")
    private Long contractId;

    @Schema(description = "签约名称")
    private String contractName;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "路由规则 ID")
    private Long routeRuleId;

    @Schema(description = "通道商户号")
    private String channelMerchantNo;

    @Schema(description = "是否默认选中")
    private Boolean selected;
}
