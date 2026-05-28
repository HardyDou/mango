package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "业务渠道模板")
public class NoticeChannelTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "业务类型ID")
    private Long businessTypeId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "标题模板")
    private String titleTemplate;

    @Schema(description = "内容模板")
    private String contentTemplate;

    @Schema(description = "三方模板 ID")
    private String channelTemplateId;

    @Schema(description = "变量映射 JSON")
    private String variableMapping;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "版本状态")
    private NoticeTemplateVersionStatus versionStatus;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "绑定渠道配置 ID，空表示 AUTO")
    private Long channelConfigId;
}
