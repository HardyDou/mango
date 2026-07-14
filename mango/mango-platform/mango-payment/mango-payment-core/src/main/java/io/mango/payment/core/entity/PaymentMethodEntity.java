package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method")
public class PaymentMethodEntity extends PaymentBaseEntity {

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

    private Integer sort;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
