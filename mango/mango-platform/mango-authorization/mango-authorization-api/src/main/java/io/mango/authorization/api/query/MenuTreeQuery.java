package io.mango.authorization.api.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单树查询条件。
 */
@Data
public class MenuTreeQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    private String appCode;
    private Integer type;
    private Long parentId;
    private String menuName;
}
