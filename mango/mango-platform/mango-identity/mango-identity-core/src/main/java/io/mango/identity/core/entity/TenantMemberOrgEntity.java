package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户成员组织岗位关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_member_org")
public class TenantMemberOrgEntity extends TenantEntity {

    private Long memberId;

    private Long postId;

    private Integer primaryFlag;

    private Integer leaderFlag;

}
