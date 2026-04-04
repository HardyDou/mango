package io.mango.user.api.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User profile entity
 * This is the business user entity, separate from system user (SysUser)
 *
 * @author Mango
 */
@Data
@TableName("biz_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID (references sys_user.user_id)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    /**
     * Real name
     */
    private String realName;

    /**
     * ID card number
     */
    private String idCard;

    /**
     * Gender (0: unknown, 1: male, 2: female)
     */
    private Integer gender;

    /**
     * Birthday
     */
    private LocalDateTime birthday;

    /**
     * Country
     */
    private String country;

    /**
     * Province
     */
    private String province;

    /**
     * City
     */
    private String city;

    /**
     * District/County
     */
    private String district;

    /**
     * Address
     */
    private String address;

    /**
     * Company name
     */
    private String company;

    /**
     * Department
     */
    private String department;

    /**
     * Job title
     */
    private String jobTitle;

    /**
     * User type (1: individual, 2: enterprise)
     */
    private Integer userType;

    /**
     * Create time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;

    /**
     * Remark
     */
    private String remark;
}
