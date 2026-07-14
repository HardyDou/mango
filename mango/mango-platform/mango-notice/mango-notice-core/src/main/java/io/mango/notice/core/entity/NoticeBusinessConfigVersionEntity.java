package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_business_config_version", excludeProperty = "orgId")
public class NoticeBusinessConfigVersionEntity extends NoticeBaseEntity {

    private Long businessTypeId;

    private String bizType;

    private String paramsSchema;

    private NoticePriority defaultPriority;

    private String idempotentStrategy;

    private Integer version;

    private NoticeTemplateVersionStatus versionStatus;

    private LocalDateTime publishTime;

    private Long publishBy;

}
