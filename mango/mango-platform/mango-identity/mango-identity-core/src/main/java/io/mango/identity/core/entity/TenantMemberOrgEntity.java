package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户成员组织岗位关系。
 */
@Data
@TableName("tenant_member_org")
public class TenantMemberOrgEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long orgId;

    private Long postId;

    private Integer primaryFlag;

    private Integer leaderFlag;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
