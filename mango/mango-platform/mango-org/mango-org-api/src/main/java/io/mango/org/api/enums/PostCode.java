package io.mango.org.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostCode implements BizCode {

    SUCCESS(200, "操作成功"),
    NOT_FOUND(404, "资源不存在"),
    VALIDATION_ERROR(400, "参数校验失败"),
    ORG_NOT_FOUND(404, "组织不存在"),
    ORG_ROOT_DELETE_FORBIDDEN(400, "根组织不能删除"),
    ORG_HAS_CHILDREN(400, "存在下级组织，不能删除"),
    ORG_PARENT_REQUIRED(400, "父级组织ID不能为空"),
    ORG_TYPE_REQUIRED(400, "组织类型不能为空"),
    ORG_TYPE_INVALID(400, "组织类型不正确"),
    ORG_CODE_REQUIRED(400, "组织编码不能为空"),
    ORG_ROOT_MANUAL_CREATE_FORBIDDEN(400, "根组织由机构初始化创建，不能手工新增"),
    ORG_PARENT_DISABLED(400, "父级组织已禁用"),
    ORG_CODE_EXISTS(400, "组织编码已存在"),
    ORG_ROOT_MOVE_FORBIDDEN(400, "根组织不能移动"),
    ORG_ROOT_DISABLE_FORBIDDEN(400, "根组织不能禁用"),
    ORG_PARENT_SELF_FORBIDDEN(400, "上级组织不能选择自己"),
    ORG_PARENT_DESCENDANT_FORBIDDEN(400, "上级组织不能选择自己的下级"),
    POST_NOT_FOUND(404, "岗位不存在"),
    POST_ID_REQUIRED(400, "岗位ID不能为空");

    private final int code;
    private final String message;
}
