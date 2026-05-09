package io.mango.org.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Organization entity
 *
 * @author Mango
 */
@Data
@TableName("sys_org")
public class SysOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Organization name
     */
    private String orgName;

    /**
     * Parent organization ID (null for root)
     */
    private Long pid;

    /**
     * Organization code (unique)
     */
    private String orgCode;

    /**
     * Organization type: 1-集团, 2-公司, 3-部门, 4-小组
     */
    private Integer orgType;

    /**
     * Sort order
     */
    private Integer orgSort;

    /**
     * Organization status: 0-disabled, 1-enabled
     */
    private String orgStatus;

    /**
     * Tenant ID for multi-tenancy
     */
    private Long tenantId;

    /**
     * Children (not stored in DB, populated by service)
     */
    @TableField(exist = false)
    private List<SysOrg> children = new ArrayList<>();
}
