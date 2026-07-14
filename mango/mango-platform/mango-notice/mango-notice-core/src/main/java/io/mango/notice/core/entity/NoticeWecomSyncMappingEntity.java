package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_wecom_sync_mapping", excludeProperty = "orgId")
public class NoticeWecomSyncMappingEntity extends NoticeBaseEntity {

    private String syncType;

    private String externalId;

    private Long localId;

    private String dataHash;

    private String displayName;

}
