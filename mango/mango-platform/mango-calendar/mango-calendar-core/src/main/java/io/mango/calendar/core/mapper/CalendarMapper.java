package io.mango.calendar.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.calendar.core.entity.Calendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CalendarMapper extends BaseMapper<Calendar> {

    @Select("SELECT * FROM calendar WHERE tenant_id = #{tenantId} AND calendar_code = #{calendarCode} LIMIT 1")
    Calendar selectByCode(@Param("tenantId") Long tenantId, @Param("calendarCode") String calendarCode);

    @Select("SELECT * FROM calendar WHERE tenant_id = #{tenantId} AND calendar_code = #{calendarCode} AND status = 1 LIMIT 1")
    Calendar selectActiveByCode(@Param("tenantId") Long tenantId, @Param("calendarCode") String calendarCode);
}
