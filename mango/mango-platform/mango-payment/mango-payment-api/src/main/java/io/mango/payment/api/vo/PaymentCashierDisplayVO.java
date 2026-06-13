package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "收银台展示配置视图")
public class PaymentCashierDisplayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Logo 文件 ID")
    private Long logoFileId;

    @Schema(description = "收银台标题，由收银台名称派生")
    private String title;

    @Schema(description = "收银台辅助说明")
    private String subtitle;

    @Schema(description = "收银台帮助文案")
    private String helpText;
}
