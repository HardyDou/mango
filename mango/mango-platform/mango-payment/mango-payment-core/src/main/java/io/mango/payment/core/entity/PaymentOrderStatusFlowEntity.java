package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_order_status_flow")
public class PaymentOrderStatusFlowEntity extends PaymentBaseEntity {

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

    private Integer delFlag;
}
