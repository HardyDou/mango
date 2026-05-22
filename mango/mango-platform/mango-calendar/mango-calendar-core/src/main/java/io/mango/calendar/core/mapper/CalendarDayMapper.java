package io.mango.calendar.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.calendar.core.entity.CalendarDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CalendarDayMapper extends BaseMapper<CalendarDay> {

    @Select("SELECT * FROM calendar_day WHERE tenant_id = #{tenantId} AND calendar_id = #{calendarId} AND calendar_date = #{date} LIMIT 1")
    CalendarDay selectByDate(@Param("tenantId") Long tenantId, @Param("calendarId") Long calendarId, @Param("date") LocalDate date);

    @Select("SELECT * FROM calendar_day WHERE tenant_id = #{tenantId} AND calendar_id = #{calendarId} AND calendar_date BETWEEN #{startDate} AND #{endDate} ORDER BY calendar_date")
    List<CalendarDay> selectBetween(@Param("tenantId") Long tenantId,
                                    @Param("calendarId") Long calendarId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM calendar_day WHERE tenant_id = #{tenantId} AND calendar_id = #{calendarId} AND calendar_year = #{year} ORDER BY calendar_date")
    List<CalendarDay> selectByYear(@Param("tenantId") Long tenantId,
                                   @Param("calendarId") Long calendarId,
                                   @Param("year") Integer year);

    @Select("SELECT COUNT(1) FROM calendar_day WHERE tenant_id = #{tenantId} AND calendar_id = #{calendarId} AND calendar_year = #{year}")
    long countByYear(@Param("tenantId") Long tenantId, @Param("calendarId") Long calendarId, @Param("year") Integer year);

    @Select("SELECT COUNT(1) FROM calendar_day WHERE tenant_id = #{tenantId} AND calendar_id = #{calendarId} AND calendar_year = #{year} AND enabled = 1")
    long countEnabledByYear(@Param("tenantId") Long tenantId, @Param("calendarId") Long calendarId, @Param("year") Integer year);
}
