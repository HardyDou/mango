package io.mango.org.api;

import io.mango.common.result.R;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.CreateOrgMemberAccountCommand;
import io.mango.org.api.command.RestoreOrgMemberAccountCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 组织管理 API 契约。
 */
@Validated
public interface SysOrgApi {

    R<List<SysOrgVO>> tree(@Valid SysOrgTreeQuery query);

    R<List<SysOrgVO>> children(
            @NotNull(message = "父级组织ID不能为空") Long parentId);

    R<SysOrgVO> getById(
            @NotNull(message = "组织ID不能为空")
            @Positive(message = "组织ID必须大于0") Long id);

    R<Long> create(@Valid CreateSysOrgCommand command);

    R<Boolean> update(@Valid UpdateSysOrgCommand command);

    R<Boolean> delete(
            @NotNull(message = "组织ID不能为空")
            @Positive(message = "组织ID必须大于0") Long id);

    R<List<OrgMemberVO>> members(
            @NotNull(message = "组织ID不能为空")
            @Positive(message = "组织ID必须大于0") Long orgId);

    R<List<Long>> memberScope(
            @NotNull(message = "组织ID不能为空")
            @Positive(message = "组织ID必须大于0") Long orgId);

    R<Long> createMemberAccount(@Valid CreateOrgMemberAccountCommand command);

    R<Long> restoreMemberAccount(@Valid RestoreOrgMemberAccountCommand command);

    R<Boolean> addMember(@Valid AddOrgMemberCommand command);

    R<Boolean> updateMember(@Valid UpdateOrgMemberCommand command);

    R<Boolean> removeMember(
            @NotNull(message = "组织成员关系ID不能为空")
            @Positive(message = "组织成员关系ID必须大于0") Long relationId);

    R<List<Long>> leaderUserIds(
            @NotNull(message = "组织ID不能为空")
            @Positive(message = "组织ID必须大于0") Long orgId);
}
