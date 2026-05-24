package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("mango_pay_method_config")
public class PaymentMethodConfig {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String methodCode;

    private String methodName;

    private String channelCode;

    private String status;

    private Long singleLimit;

    private Integer sortOrder;
}
