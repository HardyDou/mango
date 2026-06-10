package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method")
public class PaymentMethod extends AuditableEntity {

    private String methodCode;

    private String methodName;

    private Long channelId;

    private String accountNature;

    private String instrumentType;

    private String interactionType;

    private String terminalScope;

    private String paymentMaterialType;

    private String cashierGroupCode;

    private String cashierGroupName;

    private Integer cashierGroupSort;

    private Long iconFileId;

    private Integer requiresBankSelection;

    private Integer requiresQrRefresh;

    private String description;

    private String visibleScope;

    private String routeStrategy;

    private Long minAmount;

    private Long maxAmount;

    private Integer sort;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
