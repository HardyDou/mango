package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;

import java.util.List;

/**
 * Organization API interface
 *
 * @author Mango
 */
public interface SysOrgApi {

    /**
     * Get organization tree with lazy loading
     *
     * @param parentId parent organization ID (0 for root)
     * @param type organization type filter (null for all)
     * @return tree structure
     */
    R<List<SysOrg>> tree(SysOrgTreeQuery query);

    /**
     * Get children by parent ID
     *
     * @param parentId parent organization ID
     * @return children list
     */
    R<List<SysOrg>> children(Long parentId);

    /**
     * Get organization by ID
     *
     * @param id organization ID
     * @return organization
     */
    R<SysOrg> getById(Long id);
}
