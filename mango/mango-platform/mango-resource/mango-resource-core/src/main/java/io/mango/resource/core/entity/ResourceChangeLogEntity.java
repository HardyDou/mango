package io.mango.resource.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源变更日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_change_log")
public class ResourceChangeLogEntity extends TenantEntity {

    private Long resourceId;
    private String changeType;
    private Long operatorId;
    private String beforeContent;
    private String afterContent;
}
