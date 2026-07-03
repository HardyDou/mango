package io.mango.home.api.vo;

import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomeTemplateAuthorizationVO implements Serializable {

    private Long id;

    private Long templateId;

    private HomeTemplateAuthorizationSubjectType subjectType;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Boolean defaultFlag;

    private Integer sort;

    private Boolean enabled;

    private LocalDateTime createdAt;
}
