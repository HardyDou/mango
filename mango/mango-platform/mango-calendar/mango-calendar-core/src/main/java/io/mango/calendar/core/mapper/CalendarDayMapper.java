package io.mango.calendar.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.calendar.core.entity.CalendarDayEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CalendarDayMapper extends BaseMapper<CalendarDayEntity> {

    CalendarDayEntity selectByDate(@Param("tenantId") String tenantId, @Param("calendarId") Long calendarId,
                                   @Param("date") LocalDate date);

    List<CalendarDayEntity> selectBetween(@Param("tenantId") String tenantId,
                                    @Param("calendarId") Long calendarId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    List<CalendarDayEntity> selectByYear(@Param("tenantId") String tenantId,
                                   @Param("calendarId") Long calendarId,
                                   @Param("year") Integer year);

    long countByYear(@Param("tenantId") String tenantId, @Param("calendarId") Long calendarId,
                     @Param("year") Integer year);

    long countEnabledByYear(@Param("tenantId") String tenantId, @Param("calendarId") Long calendarId,
                            @Param("year") Integer year);
}
