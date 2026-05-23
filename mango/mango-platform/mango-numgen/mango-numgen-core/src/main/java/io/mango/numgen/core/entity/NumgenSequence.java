package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("numgen_sequence")
public class NumgenSequence {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String genKey;

    private Integer ruleVersion;

    private Long currentValue;

    private Integer version;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
