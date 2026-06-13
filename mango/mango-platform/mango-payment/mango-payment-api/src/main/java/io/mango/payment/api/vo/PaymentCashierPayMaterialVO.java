package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收银台支付物料视图")
public class PaymentCashierPayMaterialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付物料类型")
    private String materialType;

    @Schema(description = "二维码内容")
    private String qrContent;

    @Schema(description = "跳转地址")
    private String redirectUrl;

    @Schema(description = "HTML 表单")
    private String htmlForm;

    @Schema(description = "收款户名")
    private String accountName;

    @Schema(description = "收款账号")
    private String accountNo;

    @Schema(description = "脱敏收款账号")
    private String accountNoMask;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "转账备注")
    private String transferRemark;

    @Schema(description = "认款说明")
    private String transferInstruction;

    @Schema(description = "对账码")
    private String reconciliationCode;

    @Schema(description = "支付物料过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否需要上传凭证")
    private Boolean voucherRequired;
}
