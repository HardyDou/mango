package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticePriority;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_business_type", excludeProperty = "orgId")
public class NoticeBusinessTypeEntity extends NoticeBaseEntity {

    private String bizType;

    private String bizName;

    private String bizGroup;

    private String domainCode;

    private String description;

    private String paramsSchema;

    private Boolean enabled;

    private NoticePriority defaultPriority;

    private String idempotentStrategy;

}
