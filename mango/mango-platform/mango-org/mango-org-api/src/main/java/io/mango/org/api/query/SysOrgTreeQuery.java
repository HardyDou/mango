package io.mango.org.api.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 组织树查询条件。
 */
@Data
public class SysOrgTreeQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long parentId;

    private Integer type;
}
