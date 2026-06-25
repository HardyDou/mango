package io.mango.cms.api.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchCmsContentPublishCommand {

    @NotEmpty(message = "内容 ID 不能为空")
    @Size(max = 200, message = "内容 ID 最多200个")
    private List<Long> contentIds;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @NotEmpty(message = "栏目 ID 不能为空")
    @Size(max = 200, message = "栏目 ID 最多200个")
    private List<Long> categoryIds;

    private LocalDateTime publishTime;

    private LocalDateTime scheduledPublishTime;

    private LocalDateTime offlineTime;

    private Boolean top;

    @Pattern(regexp = "NONE|CATEGORY|SITE", message = "置顶范围不合法")
    private String topScope;

    private Boolean recommended;

    @Pattern(regexp = "NONE|HOME|HOT|EDITOR", message = "推荐类型不合法")
    private String recommendationType;

    private Integer sort;
}
