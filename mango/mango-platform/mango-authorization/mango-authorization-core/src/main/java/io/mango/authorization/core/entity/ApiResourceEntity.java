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
