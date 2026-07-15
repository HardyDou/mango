package io.mango.numgen.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("numgen_history")
public class NumgenHistoryEntity extends NumgenBaseEntity {

    private String genKey;

    private Long ruleId;

    private String resultNo;

    private Integer ruleVersion;

    private String bizKey;

    private String inputDigest;

    private Long costMillis;

    private Integer status;

    private String errorMessage;

}
