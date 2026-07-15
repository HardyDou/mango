package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("numgen_sequence")
public class NumgenSequenceEntity extends NumgenBaseEntity {

    private String genKey;

    private Integer ruleVersion;

    private String scopeKey;

    private Long currentValue;

    private Integer version;

}
