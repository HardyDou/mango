package io.mango.home.core.service;

/** 首页领域读取组织层级所需的最小能力。 */
public interface IHomeOrgProvider {

    /**
     * 查询组织的上级组织 ID。
     *
     * @param orgId 组织 ID
     * @return 上级组织 ID；组织能力不可用或无上级时返回 {@code null}
     */
    Long findParentId(Long orgId);
}
