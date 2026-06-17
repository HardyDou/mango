package io.mango.authorization.api.enums;

/**
 * 数据权限范围模式。
 */
public enum DataScopeMode {
    /**
     * 当前租户内全部数据。
     */
    ALL,
    /**
     * 当前主体自己的数据。
     */
    SELF,
    /**
     * 当前主体主组织数据。
     */
    SELF_ORG,
    /**
     * 当前主体主组织及下级组织数据。
     */
    SELF_ORG_AND_CHILDREN,
    /**
     * 指定组织范围。
     */
    ORG
}
