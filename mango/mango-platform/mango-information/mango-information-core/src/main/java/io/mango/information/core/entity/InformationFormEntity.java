package io.mango.information.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 信息采集表单实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("information_form")
public class InformationFormEntity extends TenantEntity {
    private String formCode;
    private String formName;
    private String description;
    private String definitionJson;
    private String successMessage;
    private String status;
    private String serverSecret;
    private String serverSecretDigest;
}
