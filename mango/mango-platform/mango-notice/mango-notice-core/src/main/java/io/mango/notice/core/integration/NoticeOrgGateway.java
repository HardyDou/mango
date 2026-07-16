package io.mango.notice.core.integration;

import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Isolates Notice organization synchronization from the remote result envelope. */
@Component
@RequiredArgsConstructor
public class NoticeOrgGateway {

    private final SysOrgApi sysOrgApi;

    public NoticeRemoteResult<List<SysOrg>> tree(SysOrgTreeQuery query) {
        return NoticeRemoteResult.from(sysOrgApi.tree(query))
                .map(orgs -> orgs.stream().map(SysOrg::from).toList());
    }

    public NoticeRemoteResult<SysOrg> getById(Long id) {
        return NoticeRemoteResult.from(sysOrgApi.getById(id)).map(SysOrg::from);
    }

    public NoticeRemoteResult<Long> create(CreateOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.create(command));
    }

    public NoticeRemoteResult<Void> update(UpdateOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.update(command)).map(ignored -> null);
    }

    public NoticeRemoteResult<Void> addMember(Long orgId, AddOrgMemberCommand command) {
        command.setOrgId(orgId);
        return NoticeRemoteResult.from(sysOrgApi.addMember(command)).map(ignored -> null);
    }
}
