package io.mango.home.api.vo;

import io.mango.home.api.enums.HomeTemplateVersionStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomeTemplateVersionVO implements Serializable {

    private Long id;

    private Long templateId;

    private Integer versionNo;

    private HomeTemplateVersionStatus status;

    private String layoutJson;

    private Long sourceVersionId;

    private Long publishedBy;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;
}
