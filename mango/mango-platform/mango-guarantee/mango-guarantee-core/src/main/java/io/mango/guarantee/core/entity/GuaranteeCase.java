package io.mango.guarantee.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保函业务单实体。
 */
@Data
@TableName("guarantee_case")
public class GuaranteeCase {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long caseId;

    private String caseNo;

    private Long sourceTenantId;

    private String title;

    private String applicantName;

    private String beneficiaryName;

    private String guaranteeType;

    private BigDecimal amount;

    private String currency;

    private LocalDate expectedIssueDate;

    private Integer status;

    private String remark;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
