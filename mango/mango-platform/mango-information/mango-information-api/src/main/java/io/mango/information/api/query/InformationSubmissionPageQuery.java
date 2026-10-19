package io.mango.information.api.query;

import io.mango.common.po.PageQuery;
import io.mango.information.api.enums.InformationSubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 采集记录分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InformationSubmissionPageQuery extends PageQuery {
    private Long formId;
    private InformationSubmissionStatus status;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "匹配来源页面或提交内容")
    private String keyword;

    @Schema(description = "提交时间下界，含边界")
    private LocalDateTime startTime;

    @Schema(description = "提交时间上界，含边界")
    private LocalDateTime endTime;
}
