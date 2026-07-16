package io.mango.org.starter.remote;

import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 组织管理远程适配器。
 */
@FeignClient(name = "mango-org", contextId = "orgFeignClient", path = "/org")
public interface OrgFeignClient extends SysOrgApi {

    @Override
    @GetMapping("/tree")
    R<List<SysOrgVO>> tree(@SpringQueryMap SysOrgTreeQuery query);

    @Override
    @GetMapping("/children")
    R<List<SysOrgVO>> children(@RequestParam("parentId") Long parentId);

    @Override
    @GetMapping("/detail")
    R<SysOrgVO> getById(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> create(@RequestBody CreateSysOrgCommand command);

    @Override
    @PutMapping
    R<Boolean> update(@RequestBody UpdateSysOrgCommand command);

    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);

    @Override
    @GetMapping("/members")
    R<List<OrgMemberVO>> members(@RequestParam("orgId") Long orgId);

    @Override
    @PostMapping("/members")
    R<Boolean> addMember(@RequestBody AddOrgMemberCommand command);

    @Override
    @PutMapping("/members")
    R<Boolean> updateMember(@RequestBody UpdateOrgMemberCommand command);

    @Override
    @DeleteMapping("/members")
    R<Boolean> removeMember(@RequestParam("relationId") Long relationId);

    @Override
    @GetMapping("/leader")
    R<List<Long>> leaderUserIds(@RequestParam("orgId") Long orgId);
}
