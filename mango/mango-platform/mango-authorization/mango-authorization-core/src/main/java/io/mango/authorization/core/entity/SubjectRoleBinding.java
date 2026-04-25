package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * Subject-Role relationship entity
 *
 * @author Mango
 */
@Data
@TableName("sys_user_role")
public class SubjectRoleBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Tenant ID
     */
    private Long tenantId;

    /**
     * Subject ID
     */
    @TableField("user_id")
    private Long subjectId;

    /**
     * Role ID
     */
    private Long roleId;
}
