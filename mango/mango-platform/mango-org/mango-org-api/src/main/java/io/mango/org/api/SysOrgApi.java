package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
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

    /**
     * Create organization.
     *
     * @param command create command
     * @return created organization ID
     */
    R<Long> create(CreateOrgCommand command);

    /**
     * Update organization.
     *
     * @param command update command
     * @return empty result
     */
    R<Void> update(UpdateOrgCommand command);

    /**
     * Add member to organization.
     *
     * @param orgId organization ID
     * @param command add member command
     * @return empty result
     */
    R<Void> addMember(Long orgId, AddOrgMemberCommand command);
}
