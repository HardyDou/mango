package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("numgen_history")
public class NumgenHistory {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String genKey;

    private Long ruleId;

    private String resultNo;

    private Integer ruleVersion;

    private String bizKey;

    private String inputDigest;

    private Long costMillis;

    private Integer status;

    private String errorMessage;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
