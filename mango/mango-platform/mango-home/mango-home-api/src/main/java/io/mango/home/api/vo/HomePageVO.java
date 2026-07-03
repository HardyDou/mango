package io.mango.home.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomePageVO implements Serializable {

    private Long id;

    private String routeKey;

    private String tenantId;

    private Long userId;

    private Long templateId;

    private Long templateVersionId;

    private String name;

    private String layoutJson;

    private Integer sort;

    private Boolean enabled;

    private Boolean defaultPage;

    private Boolean builtIn;

    private String sourceType;

    private String sourceLabel;

    private List<String> sourceLabels;

    private Boolean readOnly;

    private Boolean canCopy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
