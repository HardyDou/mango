package io.mango.resource.starter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceSyncLogVO {

    private Long id;
    private Long resourceId;
    private String syncType;
    private String result;
    private String message;
    private LocalDateTime createdAt;
}
