package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "通道签约配置视图")
public class PaymentChannelContractVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约配置 ID")
    private Long id;

    @Schema(description = "签约编码")
    private String contractCode;

    @Schema(description = "签约名称")
    private String contractName;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "支付通道 ID")
    private Long channelId;

    @Schema(description = "支付通道名称")
    private String channelName;

    @Schema(description = "内部路由域")
    private String environment;

    @Schema(description = "商户号")
    private String merchantNo;

    @Schema(description = "通道 AppId")
    private String appId;

    @Schema(description = "配置值 JSON")
    private String configValuesJson;

    @Schema(description = "已开通标准支付方式编码")
    private String enabledMethodCodes;

    @Schema(description = "签约能力列表")
    private List<PaymentChannelContractCapabilityVO> capabilities;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
