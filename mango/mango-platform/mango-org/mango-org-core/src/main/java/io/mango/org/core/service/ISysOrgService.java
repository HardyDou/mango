package io.mango.org.core.service;

import io.mango.infra.persistence.api.crud.MangoTypedCrudService;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.CreateOrgMemberAccountCommand;
import io.mango.org.api.command.RestoreOrgMemberAccountCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.core.entity.SysOrgEntity;

import java.util.List;

/**
 * 组织业务服务。
 */
public interface ISysOrgService extends MangoTypedCrudService<
        SysOrgEntity, CreateSysOrgCommand, UpdateSysOrgCommand, SysOrgTreeQuery, SysOrgVO, Long> {

    List<SysOrgVO> tree(SysOrgTreeQuery query);

    List<SysOrgVO> children(Long parentId);

    List<OrgMemberVO> members(Long orgId);

    List<Long> memberScope(Long orgId);

    Long createMemberAccount(CreateOrgMemberAccountCommand command);

    Long restoreMemberAccount(RestoreOrgMemberAccountCommand command);

    boolean addMember(AddOrgMemberCommand command);

    boolean updateMember(UpdateOrgMemberCommand command);

    boolean removeMember(Long relationId);

    List<Long> leaderUserIds(Long orgId);
}
