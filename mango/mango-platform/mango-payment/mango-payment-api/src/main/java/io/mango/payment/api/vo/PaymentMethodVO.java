package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付方式视图")
public class PaymentMethodVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付方式 ID")
    private Long id;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付方式名称")
    private String methodName;

    @Schema(description = "一级分类")
    private String accountNature;

    @Schema(description = "二级分类")
    private String instrumentType;

    @Schema(description = "三级分类")
    private String interactionType;

    @Schema(description = "终端范围")
    private String terminalScope;

    @Schema(description = "支付物料类型")
    private String paymentMaterialType;

    @Schema(description = "收银台展示分组编码")
    private String cashierGroupCode;

    @Schema(description = "收银台展示分组名称")
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

    @Schema(description = "可见范围")
    private String visibleScope;

    @Schema(description = "路由策略说明")
    private String routeStrategy;

    @Schema(description = "单笔最小金额，单位分")
    private Long minAmount;

    @Schema(description = "单笔最大金额，单位分")
    private Long maxAmount;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
