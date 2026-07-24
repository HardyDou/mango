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

    private Long id;
    private NoticeChannelType channelType;
    private String tagCode;
    private String tagName;
    private String description;
    private Integer candidateCount;
    private List<String> candidateConfigNames;
}
