package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("numgen_generator")
public class NumgenGeneratorEntity extends NumgenBaseEntity {

    private String genKey;

    private String genName;

    private String domainCode;

    private Integer status;

    private Integer currentRuleVersion;

    private Integer currentPublishStatus;

    @TableLogic
    private Integer delFlag = 0;
}
