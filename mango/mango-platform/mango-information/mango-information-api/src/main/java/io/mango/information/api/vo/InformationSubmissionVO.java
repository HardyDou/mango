package io.mango.information.api.vo;

import io.mango.information.api.enums.InformationSubmissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 信息采集提交记录。 */
@Data
public class InformationSubmissionVO {
    private Long id;
    private Long formId;
    private String formCode;
    private String formName;
    private String definitionJson;
    private String valuesJson;
    private String sourcePage;
    private InformationSubmissionStatus status;
    private String processRemark;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
