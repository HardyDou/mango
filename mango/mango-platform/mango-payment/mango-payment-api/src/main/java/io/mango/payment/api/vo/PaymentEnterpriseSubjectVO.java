package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收款主体视图")
public class PaymentEnterpriseSubjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主体 ID")
    private Long id;

    @Schema(description = "主体名称")
    private String subjectName;

    @Schema(description = "统一社会信用代码脱敏值")
    private String creditCodeMask;

    @Schema(description = "银行账户脱敏值")
    private String bankAccountNoMask;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "证照文件 ID")
    private Long licenseFileId;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
