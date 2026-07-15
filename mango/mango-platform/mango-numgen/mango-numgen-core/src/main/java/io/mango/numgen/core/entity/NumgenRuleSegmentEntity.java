package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("numgen_rule_segment")
public class NumgenRuleSegmentEntity extends NumgenBaseEntity {

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

}
