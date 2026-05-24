package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("mango_pay_manage_domain")
public class PaymentManageDomain {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String code;

    private String title;

    private String description;

    private String badge;

    private Integer sortOrder;
}
