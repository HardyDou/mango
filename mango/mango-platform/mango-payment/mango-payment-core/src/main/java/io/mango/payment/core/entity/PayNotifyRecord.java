package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mango_pay_notify_record")
public class PayNotifyRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String notifyEventId;

    private Long paymentOrderId;

    private String channelOrderNo;

    private String rawRequest;

    private Integer verified;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
