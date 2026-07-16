package io.mango.identity.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 机构成员分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机构成员分页查询条件")
public class IdentityUserPageQuery extends PageQuery {

    @Schema(description = "用户名，支持模糊查询")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;

    @Schema(description = "关键字，匹配用户名、昵称、手机号或邮箱")
    @Size(max = 100, message = "关键字最多100个字符")
    private String keyword;

    @Schema(description = "昵称，支持模糊查询")
    @Size(max = 100, message = "昵称最多100个字符")
    private String nickname;

    @Schema(description = "手机号，支持模糊查询")
    @Size(max = 32, message = "手机号最多32个字符")
    private String phone;

    @Schema(description = "邮箱，支持模糊查询")
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;

    @Schema(description = "成员状态：0-禁用，1-启用")
    @Min(value = 0, message = "成员状态只能为0或1")
    @Max(value = 1, message = "成员状态只能为0或1")
    private Integer status;

    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "操作者类型，例如 INTERNAL_USER")
    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Schema(description = "归属主体类型，例如 INTERNAL_ORG")
    @Size(max = 64, message = "归属主体类型最多64个字符")
    private String partyType;

    @Schema(description = "归属主体ID")
    @Positive(message = "归属主体ID必须大于0")
    private Long partyId;

    @Schema(description = "组织ID。传入后只查询该组织下的成员")
    @Positive(message = "组织ID必须大于0")
    private Long orgId;
}
