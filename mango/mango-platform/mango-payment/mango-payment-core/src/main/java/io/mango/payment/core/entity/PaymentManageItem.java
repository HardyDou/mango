package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mango_pay_manage_item")
public class PaymentManageItem {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String domain;

    private String code;

    private String name;

    private String owner;

    private String status;

    private String primaryText;

    private String secondaryText;

    private Integer sortOrder;

    private LocalDateTime updatedAt;
}
