package io.mango.calendar.core.service;

import io.mango.calendar.api.query.LunarDateQuery;
import io.mango.calendar.api.query.SolarDateQuery;
import io.mango.calendar.api.query.SolarTermYearQuery;
import io.mango.calendar.api.vo.LunarDayInfoVO;
import io.mango.calendar.api.vo.SolarTermVO;
import io.mango.calendar.core.entity.CalendarDay;

import java.time.LocalDate;
import java.util.List;

public interface ICalendarLunarService {

    LunarDayInfoVO getLunarDay(SolarDateQuery query);

    LocalDate lunarToSolar(LunarDateQuery query);

    List<SolarTermVO> listSolarTerms(SolarTermYearQuery query);

    void applyLunarInfo(CalendarDay day);
}
