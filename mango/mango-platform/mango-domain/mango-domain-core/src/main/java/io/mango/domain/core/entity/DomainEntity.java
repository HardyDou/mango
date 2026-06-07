package io.mango.domain.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务域实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_domain")
public class DomainEntity extends TenantEntity {

    private String domainCode;
    private String domainShortCode;
    private String domainName;
    private Long parentId;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
