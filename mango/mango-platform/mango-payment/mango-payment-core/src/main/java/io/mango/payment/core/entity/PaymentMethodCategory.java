package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method_category")
public class PaymentMethodCategory extends AuditableEntity {

    private String categoryCode;

    private String categoryName;

    private Integer level;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private Long tenantId;

    @TableLogic
    private Integer delFlag = 0;
}
