package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付敏感字段历史重加密结果。
 */
@Data
@Schema(description = "支付敏感字段历史重加密结果")
public class PaymentSensitiveFieldReencryptResultVO {

    @Schema(description = "应用密钥重加密数量")
    private int applicationSecretCount;

    @Schema(description = "企业主体证件号重加密数量")
    private int enterpriseCreditCodeCount;

    @Schema(description = "企业主体银行账号重加密数量")
    private int enterpriseBankAccountCount;

    @Schema(description = "主体银行账户账号重加密数量")
    private int subjectBankAccountCount;

    @Schema(description = "本轮合计重加密数量")
    private int totalCount;

    @Schema(description = "本轮批量上限")
    private int limit;
}
