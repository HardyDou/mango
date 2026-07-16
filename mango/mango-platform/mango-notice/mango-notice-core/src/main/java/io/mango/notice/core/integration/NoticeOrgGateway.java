package io.mango.notice.core.integration;

import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/** Isolates Notice organization synchronization from the remote result envelope. */
@Component
public class NoticeOrgGateway {

    private final Supplier<SysOrgApi> sysOrgApi;

    public NoticeOrgGateway(SysOrgApi sysOrgApi) {
        this.sysOrgApi = () -> sysOrgApi;
    }

    public NoticeRemoteResult<List<SysOrg>> tree(SysOrgTreeQuery query) {
        return NoticeRemoteResult.from(sysOrgApi.get().tree(query))
                .map(orgs -> orgs.stream().map(SysOrg::from).toList());
    }

    public NoticeRemoteResult<SysOrg> getById(Long id) {
        return NoticeRemoteResult.from(sysOrgApi.get().getById(id)).map(SysOrg::from);
    }

    public NoticeRemoteResult<Long> create(CreateOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.get().create(command));
    }

    public NoticeRemoteResult<Void> update(UpdateOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.get().update(command)).map(ignored -> null);
    }

    public NoticeRemoteResult<Void> addMember(Long orgId, AddOrgMemberCommand command) {
        command.setOrgId(orgId);
        return NoticeRemoteResult.from(sysOrgApi.get().addMember(command)).map(ignored -> null);
    }
}
