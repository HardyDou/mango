package io.mango.calendar.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.calendar.core.entity.CalendarEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalendarMapper extends BaseMapper<CalendarEntity> {

    CalendarEntity selectByCode(@Param("tenantId") String tenantId, @Param("calendarCode") String calendarCode);

    CalendarEntity selectActiveByCode(@Param("tenantId") String tenantId, @Param("calendarCode") String calendarCode);
}
