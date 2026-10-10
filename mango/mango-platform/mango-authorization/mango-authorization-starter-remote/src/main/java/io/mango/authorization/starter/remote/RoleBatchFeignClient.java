package io.mango.authorization.starter.remote;

import io.mango.authorization.api.RoleBatchApi;
import io.mango.authorization.api.command.BatchRoleAssignmentCommand;
import io.mango.authorization.api.vo.BatchRoleAssignmentPreviewVO;
import io.mango.authorization.api.vo.BatchRoleAssignmentResultVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 租户范围批量角色绑定远程客户端。 */
@FeignClient(name = "mango-authorization", contextId = "roleBatchFeignClient", path = "/authorization/roles/batch")
public interface RoleBatchFeignClient extends RoleBatchApi {

    @Override
    @PostMapping("/preview")
    R<BatchRoleAssignmentPreviewVO> preview(@RequestBody BatchRoleAssignmentCommand command);

    @Override
    @PostMapping("/assign")
    R<BatchRoleAssignmentResultVO> assign(@RequestBody BatchRoleAssignmentCommand command);

    @Override
    @PostMapping("/unassign")
    R<BatchRoleAssignmentResultVO> unassign(@RequestBody BatchRoleAssignmentCommand command);
}
