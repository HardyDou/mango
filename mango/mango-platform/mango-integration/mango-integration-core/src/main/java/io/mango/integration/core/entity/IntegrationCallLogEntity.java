package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成调用记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_call_log")
public class IntegrationCallLogEntity extends TenantEntity {

    private String instanceCode;
    private String connectorCode;
    private String httpMethod;
    private String path;
    private Integer statusCode;
    private Long elapsedMs;
    private Integer success;
    private String message;
}
