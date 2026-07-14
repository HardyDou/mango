package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_mango_pay_scenario_control")
public class PaymentMangoPayScenarioControlEntity extends PaymentBaseEntity {

    private String controlNo;

    private String channelCode;

    private Long contractId;

    private String scenarioType;

    private String scenarioCode;

    private String billDifferenceType;

    private Long differenceAmount;

    private Integer callbackDelayMinutes;

    private Integer effectiveCount;

    private Integer consumedCount;

    private String status;

    private LocalDateTime consumedAt;

    private String remark;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
