package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * API 资源实体。
 */
@Data
@TableName("authorization_api_resource")
@EqualsAndHashCode(callSuper = true)
public class ApiResourceEntity extends AuthorizationBaseEntity {

    private String moduleName;

    private String domain;

    private String source;

    /** 是否人工接管：0-否，1-是。 */
    private Integer manualOverride;

    /** 接管前的登记来源。 */
    private String originSource;

    /** 接管前的访问模式。 */
    private String originAccessMode;

    /** 接管前的权限标识。 */
    private String originPermissionCode;

    /** 接管前的资源描述。 */
    private String originDescription;

    private String httpMethod;

    private String pathPattern;

    private String resourceCode;

    private String permissionCode;

    private String accessMode;

    private String handlerClass;

    private String handlerMethod;

    private String description;

    private Integer status;

    @TableLogic
    private Integer deleted;
}
