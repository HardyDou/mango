package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_order_status_flow")
public class PaymentOrderStatusFlowEntity extends AuditableEntity {

    private String orderType;

    private Long orderId;

    private String orderNo;

    private String fromStatus;

    private String toStatus;

    private String triggerSource;

    private String triggerNo;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime happenTime;

    private String remark;

    private Long tenantId;

    private Integer delFlag;
}
