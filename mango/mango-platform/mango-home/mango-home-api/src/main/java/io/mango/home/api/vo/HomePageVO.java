package io.mango.home.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomePageVO implements Serializable {

    private Long id;

    private String tenantId;

    private Long userId;

    private String name;

    private String layoutJson;

    private Integer sort;

    private Boolean enabled;

    private Boolean defaultPage;

    private Boolean builtIn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
