package io.mango.org.core.service;

import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;

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
     * Get organization tree with optional disabled nodes.
     *
     * @param parentId parent organization ID (0 for root)
     * @param type organization type filter (null for all)
     * @param includeDisabled whether disabled organizations should be returned
     * @return tree structure
     */
    List<SysOrg> tree(Long parentId, Integer type, boolean includeDisabled);

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

    /**
     * Create organization.
     *
     * @param command create command
     * @return created organization ID
     */
    Long create(CreateOrgCommand command);

    /**
     * Update organization.
     *
     * @param command update command
     */
    void update(UpdateOrgCommand command);

    /**
     * Delete organization by ID.
     *
     * @param id organization ID
     */
    void delete(Long id);
}
