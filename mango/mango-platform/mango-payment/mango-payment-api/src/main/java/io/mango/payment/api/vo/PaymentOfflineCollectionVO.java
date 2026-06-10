package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "线下收款")
public class PaymentOfflineCollectionVO {

    @Schema(description = "线下收款 ID")
    private Long id;

    @Schema(description = "线下收款单号")
    private String offlineCollectionNo;

    @Schema(description = "支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付标题")
    private String title;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "通道 ID")
    private Long channelId;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "通道签约配置 ID")
    private Long contractId;

    @Schema(description = "通道签约配置名称")
    private String contractName;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "收款银行账户 ID")
    private Long bankAccountId;

    @Schema(description = "收款户名")
    private String accountName;

    @Schema(description = "脱敏收款账号")
    private String accountNoMask;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "收款金额，单位分")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "用户提交转账金额，单位分")
    private Long transferAmount;

    @Schema(description = "转账凭证文件 ID，多个用英文逗号分隔")
    private String voucherFileIds;

    @Schema(description = "用户提交凭证时间")
    private LocalDateTime submittedTime;

    @Schema(description = "用户提交说明")
    private String submitRemark;

    @Schema(description = "确认到账金额，单位分")
    private Long confirmedAmount;

    @Schema(description = "确认人 ID")
    private Long confirmedBy;

    @Schema(description = "确认人名称")
    private String confirmedByName;

    @Schema(description = "确认说明")
    private String confirmRemark;

    @Schema(description = "随机对账码")
    private String reconciliationCode;

    @Schema(description = "转账备注")
    private String transferRemark;

    @Schema(description = "凭证数量")
    private Integer voucherCount;

    @Schema(description = "线下收款状态编码")
    private String collectionStatus;

    @Schema(description = "线下收款状态名称")
    private String collectionStatusName;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "确认到账时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
