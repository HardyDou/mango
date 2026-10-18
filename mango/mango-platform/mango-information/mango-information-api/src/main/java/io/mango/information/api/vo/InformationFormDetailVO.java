package io.mango.information.api.vo;

import io.mango.information.api.enums.InformationFormStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 信息采集表单详情。 */
@Data
public class InformationFormDetailVO {
    private Long id;
    private String formCode;
    private String formName;
    private String description;
    private String definitionJson;
    private String successMessage;
    private InformationFormStatus status;
    private String serverSecret;
    private String hostedPath;
    private Long submissionCount;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
