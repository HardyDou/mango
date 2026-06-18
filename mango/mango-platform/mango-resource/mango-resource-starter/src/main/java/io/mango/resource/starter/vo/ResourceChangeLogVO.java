package io.mango.resource.starter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceChangeLogVO {

    private Long id;
    private Long resourceId;
    private String changeType;
    private Long operatorId;
    private String beforeContent;
    private String afterContent;
    private LocalDateTime createdAt;
}
