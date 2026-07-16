package io.mango.org.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Organization entity
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrgEntity extends OrgBaseEntity {

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

}
