package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method_category")
public class PaymentMethodCategoryEntity extends PaymentBaseEntity {

    private String categoryCode;

    private String categoryName;

    private Integer level;

    private Long parentId;

    private Integer sort;

    private Integer status;

    @TableLogic
    private Integer delFlag = 0;
}
