package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户成员实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_member")
public class TenantMemberEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 全局账号 ID。 */
    private Long userId;

    /** 成员编号。 */
    private String memberNo;

    /** 成员显示名称。 */
    private String displayName;

    /** 成员类型。 */
    private String memberType;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 主组织 ID。 */
    private Long primaryOrgId;

    /** 主岗位 ID。 */
    private Long primaryPostId;

    /** 加入时间。 */
    private LocalDateTime joinedAt;

    /** 离开时间。 */
    private LocalDateTime leftAt;

    /** 备注。 */
    private String remark;

    public Long getMemberId() {
        return getId();
    }

    public void setMemberId(Long memberId) {
        setId(memberId);
    }
}
