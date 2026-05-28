package io.mango.identity.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构成员分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机构成员分页查询条件")
public class IdentityUserPageQuery extends PageQuery {

    @Schema(description = "用户名，支持模糊查询")
    private String username;

    @Schema(description = "关键字，匹配用户名、昵称、手机号或邮箱")
    private String keyword;

    @Schema(description = "昵称，支持模糊查询")
    private String nickname;

    @Schema(description = "手机号，支持模糊查询")
    private String phone;

    @Schema(description = "邮箱，支持模糊查询")
    private String email;

    @Schema(description = "成员状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER")
    private String realm;

    @Schema(description = "操作者类型，例如 INTERNAL_USER")
    private String actorType;

    @Schema(description = "归属主体类型，例如 INTERNAL_ORG")
    private String partyType;

    @Schema(description = "归属主体ID")
    private Long partyId;

    @Schema(description = "组织ID。传入后只查询该组织下的成员")
    private Long orgId;
}
