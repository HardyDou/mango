package io.mango.information.api.vo;

import io.mango.information.api.enums.InformationFormStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 信息采集表单列表项。 */
@Data
public class InformationFormVO {
    private Long id;
    private String formCode;
    private String formName;
    private String description;
    private InformationFormStatus status;
    private Integer fieldCount;
    private Long submissionCount;
    private String hostedPath;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
