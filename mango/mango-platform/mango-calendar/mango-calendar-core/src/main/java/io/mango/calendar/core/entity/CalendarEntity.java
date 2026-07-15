package io.mango.calendar.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("calendar")
public class CalendarEntity extends TenantEntity {

    private String calendarCode;

    private String calendarName;

    private Integer status;

}
