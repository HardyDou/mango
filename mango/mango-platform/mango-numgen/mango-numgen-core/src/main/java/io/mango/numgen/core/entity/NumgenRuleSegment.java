package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("numgen_rule_segment")
public class NumgenRuleSegment {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private Integer sortOrder;

    private String segmentType;

    private String segmentName;

    private String literalValue;

    private String variableKey;

    private String dateFormat;

    private Integer seqWidth;

    private String padChar;

    private Integer sequenceScope;

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
