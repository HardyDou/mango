package io.mango.notice.core.integration;

import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.api.query.SysOrgTreeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Isolates Notice organization synchronization from the remote result envelope. */
@Component
@RequiredArgsConstructor
public class NoticeOrgGateway {

    private final SysOrgApi sysOrgApi;

    public NoticeRemoteResult<List<SysOrgVO>> tree(SysOrgTreeQuery query) {
        return NoticeRemoteResult.from(sysOrgApi.tree(query));
    }

    public NoticeRemoteResult<SysOrgVO> getById(Long id) {
        return NoticeRemoteResult.from(sysOrgApi.getById(id));
    }

    public NoticeRemoteResult<Long> create(CreateSysOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.create(command));
    }

    public NoticeRemoteResult<Boolean> update(UpdateSysOrgCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.update(command));
    }

    public NoticeRemoteResult<Boolean> addMember(AddOrgMemberCommand command) {
        return NoticeRemoteResult.from(sysOrgApi.addMember(command));
    }
}
