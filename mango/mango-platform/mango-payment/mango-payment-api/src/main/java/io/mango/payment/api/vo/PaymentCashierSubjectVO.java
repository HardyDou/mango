package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "收银台收款主体视图")
public class PaymentCashierSubjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "企业主体 ID")
    private Long id;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "银行账号")
    private String bankAccountNo;

    @Schema(description = "开户行")
    private String bankName;
}
