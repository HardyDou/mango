package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成连接实例实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_instance")
public class IntegrationInstanceEntity extends TenantEntity {

    private String instanceCode;
    private String instanceName;
    private String connectorCode;
    private String environment;
    private String configValuesJson;
    private String credentialCode;
    private String capabilitiesJson;
    private Integer status;
    private Integer revision;
    private String owner;
    private String remark;
}
