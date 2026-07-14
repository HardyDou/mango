package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_setting", excludeProperty = {
        "orgId", "createdBy", "createdAt", "updatedBy"
})
public class NoticeSettingEntity extends NoticeBaseEntity {

    private String settingKey;

    private String settingValue;

}
