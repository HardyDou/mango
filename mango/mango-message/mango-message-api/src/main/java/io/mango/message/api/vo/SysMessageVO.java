package io.mango.message.api.vo;

import io.mango.message.api.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysMessageVO {
    private Long id;
    private MessageType messageType;
    private String title;
    private String content;
    private Long userId;
    private String username;
    private Integer priority;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
