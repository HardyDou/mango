package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "同步企业微信用户命令")
public class SyncWecomUsersCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "企业微信渠道配置 ID；为空时使用第一个启用的企业微信渠道配置")
    private Long channelConfigId;

    @Schema(description = "企业微信 CorpId；为空时从渠道配置读取")
    @Size(max = 128, message = "企业微信 CorpId 最多128个字符")
    private String corpId;

    @Schema(description = "企业微信通讯录 Secret；为空时从渠道配置读取")
    @Size(max = 256, message = "企业微信通讯录 Secret 最多256个字符")
    private String secret;

    @Schema(description = "企业微信部门 ID；为空时按同步目标自动解析")
    private Long departmentId;

    @Schema(description = "Mango 同步目标组织 ID；公司同步时作为组织挂载点，部门同步时作为成员归属部门")
    private Long targetOrgId;

    @Schema(description = "Mango 同步目标组织类型：1-集团，2-公司，3-部门，4-小组")
    private Integer targetOrgType;

    @Schema(description = "是否同步子部门成员")
    private Boolean fetchChild = true;

    @Schema(description = "是否同步组织架构")
    private Boolean syncDepartments = true;

    @Schema(description = "是否同步成员")
    private Boolean syncUsers = true;

    @Schema(description = "已同步且数据未变化时是否跳过")
    private Boolean skipUnchanged = true;

    @Schema(description = "是否创建缺失成员")
    private Boolean createMissingUsers = true;

    @Schema(description = "是否更新已匹配成员资料")
    private Boolean updateMatchedUsers = true;

    @Schema(description = "是否绑定企业微信通知接收账户")
    private Boolean bindNoticeAccount = true;

    @Schema(description = "是否绑定企业微信登录身份")
    private Boolean bindLoginIdentity = true;
}
