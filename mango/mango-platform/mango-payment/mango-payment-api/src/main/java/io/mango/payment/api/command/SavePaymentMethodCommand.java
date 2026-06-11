package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存支付方式命令")
public class SavePaymentMethodCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付方式 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "支付方式编码不能为空")
    @Size(max = 64, message = "支付方式编码不能超过64个字符")
    @Schema(description = "支付方式编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodCode;

    @NotBlank(message = "支付方式名称不能为空")
    @Size(max = 128, message = "支付方式名称不能超过128个字符")
    @Schema(description = "支付方式名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodName;

    @NotBlank(message = "一级分类不能为空")
    @Size(max = 32, message = "一级分类不能超过32个字符")
    @Schema(description = "一级分类：CORPORATE/PERSONAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNature;

    @NotBlank(message = "二级分类不能为空")
    @Size(max = 32, message = "二级分类不能超过32个字符")
    @Schema(description = "二级分类：WECHAT/ALIPAY/UNIONPAY/BANK_CARD/WALLET/EBANK/OFFLINE_TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instrumentType;

    @NotBlank(message = "交互形态不能为空")
    @Size(max = 32, message = "交互形态不能超过32个字符")
    @Schema(description = "三级分类：QR_CODE/H5_REDIRECT/MINIAPP/DEBIT_QUICK/CREDIT_QUICK/WALLET_QUICK/BANK_GATEWAY/ACCOUNT_TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String interactionType;

    @NotBlank(message = "终端范围不能为空")
    @Size(max = 64, message = "终端范围不能超过64个字符")
    @Schema(description = "终端范围，例如 WEB,H5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalScope;

    @NotBlank(message = "支付物料类型不能为空")
    @Size(max = 32, message = "支付物料类型不能超过32个字符")
    @Schema(description = "支付物料类型：QR/REDIRECT_URL/HTML_FORM/TRANSFER_ACCOUNT/H5_PARAM", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paymentMaterialType;

    @NotBlank(message = "收银台展示分组编码不能为空")
    @Size(max = 64, message = "收银台展示分组编码不能超过64个字符")
    @Schema(description = "收银台展示分组编码，例如 WECHAT_PAY/ALIPAY/EBANK/OFFLINE_TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cashierGroupCode;

    @NotBlank(message = "收银台展示分组名称不能为空")
    @Size(max = 128, message = "收银台展示分组名称不能超过128个字符")
    @Schema(description = "收银台展示分组名称，例如 微信支付/支付宝/网银支付/线下转账", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cashierGroupName;

    @Schema(description = "收银台展示分组排序")
    private Integer cashierGroupSort;

    @Schema(description = "图标文件 ID")
    private Long iconFileId;

    @Schema(description = "是否需要银行列表：1-需要，0-不需要")
    private Integer requiresBankSelection;

    @Schema(description = "二维码是否支持刷新：1-支持，0-不支持")
    private Integer requiresQrRefresh;

    @Schema(description = "收银台说明")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
