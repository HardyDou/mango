package io.mango.org.core.service;

import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.vo.OrgMemberVO;

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

    /**
     * Query organization members.
     *
     * @param orgId organization ID
     * @return organization members
     */
    List<OrgMemberVO> members(Long orgId);

    /**
     * Add a member to organization.
     *
     * @param orgId organization ID
     * @param command add command
     */
    void addMember(Long orgId, AddOrgMemberCommand command);

    /**
     * Update organization member relation.
     *
     * @param command update command
     */
    void updateMember(UpdateOrgMemberCommand command);

    /**
     * Remove member from organization.
     *
     * @param relationId relation ID
     */
    void removeMember(Long relationId);

    /**
     * Resolve organization leader user IDs by organization ID.
     *
     * @param orgId organization ID
     * @return leader user IDs
     */
    List<Long> leaderUserIds(Long orgId);
}
