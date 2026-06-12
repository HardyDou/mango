package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "收银台会话视图")
public class PaymentCashierSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收银台配置 ID")
    private Long cashierConfigId;

    @Schema(description = "收银台配置名称")
    private String cashierName;

    @Schema(description = "是否后台预览会话")
    private Boolean preview;

    @Schema(description = "收银台状态")
    private String status;

    @Schema(description = "默认支付方式编码")
    private String defaultMethodCode;

    @Schema(description = "支付成功后返回地址。优先业务订单返回地址，其次收银台结果返回地址")
    private String returnUrl;

    @Schema(description = "服务端当前时间")
    private LocalDateTime serverTime;

    @Schema(description = "展示配置")
    private PaymentCashierDisplayVO display;

    @Schema(description = "应用信息")
    private PaymentCashierApplicationVO application;

    @Schema(description = "订单信息")
    private PaymentCashierOrderVO order;

    @Schema(description = "收款主体")
    private PaymentCashierSubjectVO subject;

    @Schema(description = "可用支付方式")
    private List<PaymentCashierMethodVO> methods = new ArrayList<>();
}
