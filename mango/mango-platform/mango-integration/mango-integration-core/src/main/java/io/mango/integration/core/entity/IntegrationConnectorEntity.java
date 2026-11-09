package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成连接器定义实体。
 *
 * <p>定义随平台版本发布，租户不可编辑，只作为实例与页面的规范来源。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_connector")
public class IntegrationConnectorEntity extends TenantEntity {

    private String connectorCode;
    private String connectorName;
    private String category;
    private String provider;
    private Integer status;
    private String source;
    private Integer revision;
    private String configSchema;
    private String authSchema;
    private String capabilitiesJson;
    private String tags;
    private String docsUrl;
}
