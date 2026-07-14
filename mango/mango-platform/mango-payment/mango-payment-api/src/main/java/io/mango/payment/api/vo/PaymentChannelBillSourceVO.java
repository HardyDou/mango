package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付通道账单源")
public class PaymentChannelBillSourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账单源 ID")
    private Long id;
    @Schema(description = "通道签约配置 ID")
    private Long contractId;
    @Schema(description = "通道签约配置名称")
    private String contractName;
    @Schema(description = "支付通道 ID")
    private Long channelId;
    @Schema(description = "支付通道名称")
    private String channelName;
    @Schema(description = "企业主体 ID")
    private Long subjectId;
    @Schema(description = "企业主体名称")
    private String subjectName;
    @Schema(description = "通道商户号")
    private String merchantNo;
    @Schema(description = "支付通道编码")
    private String channelCode;
    @Schema(description = "账单获取模式")
    private String fetchMode;
    @Schema(description = "账单获取模式名称")
    private String fetchModeName;
    @Schema(description = "账单服务地址")
    private String endpoint;
    @Schema(description = "账单远端路径")
    private String remotePath;
    @Schema(description = "账单凭据引用")
    private String credentialRef;
    @Schema(description = "账单分页模式")
    private String pageMode;
    @Schema(description = "是否启用")
    private Integer enabled;
    @Schema(description = "启用状态名称")
    private String enabledName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
