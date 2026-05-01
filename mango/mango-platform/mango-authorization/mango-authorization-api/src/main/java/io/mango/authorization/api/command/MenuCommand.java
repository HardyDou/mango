package io.mango.authorization.api.command;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单创建或修改命令。
 */
@Data
public class MenuCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long menuId;
    private String appCode;
    private Long parentId;
    private Integer menuType;
    private String menuName;
    private String menuCode;
    private String path;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer visible;
    private String component;
    private Integer keepAlive;
    private Integer embedded;
    private String redirect;
    private String permissions;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
    private Integer delFlag;
}
