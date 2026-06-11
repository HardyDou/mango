package io.mango.payment.api.vo;

import io.mango.payment.api.enums.PaymentChannelCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "支付通道视图")
public class PaymentChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通道 ID")
    private Long id;

    @Schema(description = "通道编码")
    private PaymentChannelCode channelCode;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "通道类型")
    private String channelType;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "通道基础网关地址")
    private String gatewayBaseUrl;

    @Schema(description = "签约字段模板 JSON")
    private String fieldTemplateJson;

    @Schema(description = "通道能力摘要")
    private String capabilitySummary;

    @Schema(description = "支持的账单获取方式：MANUAL、FTP、FTPS、HTTP")
    private List<String> billFetchModes;

    @Schema(description = "通道能力列表")
    private List<PaymentChannelCapabilityVO> capabilities;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
