package io.mango.org.core.service;

import io.mango.org.api.entity.SysOrg;

import java.util.List;

/**
 * Organization service interface (internal use only)
 *
 * @author Mango
 */
public interface ISysOrgService {

    /**
     * Get organization tree with lazy loading
     *
     * @param parentId parent organization ID (0 for root)
     * @param type organization type filter (null for all)
     * @return tree structure
     */
    List<SysOrg> tree(Long parentId, Integer type);

    /**
     * Get children by parent ID
     *
     * @param parentId parent organization ID
     * @return children list
     */
    List<SysOrg> children(Long parentId);

    /**
     * Get organization by ID
     *
     * @param id organization ID
     * @return organization
     */
    SysOrg getById(Long id);
}
