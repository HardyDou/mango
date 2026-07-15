package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("numgen_rule")
public class NumgenRuleEntity extends NumgenBaseEntity {

    private String genKey;

    private String ruleName;

    private Integer version;

    private Integer status;

    private Integer publishStatus;

    private String versionState;

    @TableLogic
    private Integer delFlag = 0;
}
