package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_order")
public class PaymentOrderEntity extends AuditableEntity {

    private String payOrderNo;

    private Long businessOrderId;

    private Long cashierConfigId;

    private Long channelId;

    private String channelCode;

    private String channelMerchantNo;

    private Long contractId;

    private Long contractCapabilityId;

    private Long routeRuleId;

    private Long methodId;

    private Long amount;

    private String status;

    private String channelTradeNo;

    private String paymentMaterialJson;

    private Integer successFlag;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    private Long tenantId;
}
