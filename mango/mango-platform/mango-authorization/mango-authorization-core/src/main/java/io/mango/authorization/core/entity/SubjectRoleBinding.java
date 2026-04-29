package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 主体角色关系实体。
 */
@Data
@TableName("authorization_subject_role")
public class SubjectRoleBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID。 */
    private Long tenantId;

    /** 主体 ID。 */
    @TableField("subject_id")
    private Long subjectId;

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 归属主体类型。 */
    private String partyType;

    /** 归属主体 ID。 */
    private Long partyId;

    /** 角色 ID。 */
    private Long roleId;
}
