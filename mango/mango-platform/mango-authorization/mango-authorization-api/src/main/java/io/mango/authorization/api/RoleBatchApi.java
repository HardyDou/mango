package io.mango.authorization.api;

import io.mango.authorization.api.command.BatchRoleAssignmentCommand;
import io.mango.authorization.api.vo.BatchRoleAssignmentPreviewVO;
import io.mango.authorization.api.vo.BatchRoleAssignmentResultVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;

/** 租户范围批量角色绑定 API 契约。 */
public interface RoleBatchApi {

    /** 预览符合目标范围的启用成员。 */
    R<BatchRoleAssignmentPreviewVO> preview(@Valid BatchRoleAssignmentCommand command);

    /** 幂等地为目标成员绑定角色。 */
    R<BatchRoleAssignmentResultVO> assign(@Valid BatchRoleAssignmentCommand command);

    /** 按同一目标范围批量解除角色绑定，用于回滚或撤销。 */
    R<BatchRoleAssignmentResultVO> unassign(@Valid BatchRoleAssignmentCommand command);
}
