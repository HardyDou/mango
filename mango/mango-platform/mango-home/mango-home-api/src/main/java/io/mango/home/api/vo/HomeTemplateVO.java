package io.mango.home.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomeTemplateVO implements Serializable {

    private Long id;

    private String tenantId;

    private String name;

    private Boolean enabled;

    private Long activeVersionId;

    private Integer activeVersionNo;

    private String activeLayoutJson;

    private Long draftVersionId;

    private String draftLayoutJson;

    private Integer authorizationCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
