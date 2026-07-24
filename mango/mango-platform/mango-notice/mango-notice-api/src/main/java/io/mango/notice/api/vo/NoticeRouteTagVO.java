package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "通知渠道路由标签")
public class NoticeRouteTagVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签 ID")
    private Long id;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "标签编码")
    private String tagCode;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签说明")
    private String description;

    @Schema(description = "当前可用候选账号数量")
    private Integer candidateCount;

    @Schema(description = "当前可用候选账号名称")
    private List<String> candidateConfigNames;

    public List<String> getCandidateConfigNames() {
        return candidateConfigNames == null ? null : List.copyOf(candidateConfigNames);
    }

    public void setCandidateConfigNames(List<String> candidateConfigNames) {
        this.candidateConfigNames =
                candidateConfigNames == null ? null : List.copyOf(candidateConfigNames);
    }
}
