package io.mango.calendar.core.service.impl;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import io.mango.calendar.api.query.LunarDateQuery;
import io.mango.calendar.api.query.SolarDateQuery;
import io.mango.calendar.api.query.SolarTermYearQuery;
import io.mango.calendar.api.vo.LunarDayInfoVO;
import io.mango.calendar.api.vo.SolarTermVO;
import io.mango.calendar.core.entity.CalendarDay;
import io.mango.calendar.core.service.ICalendarLunarService;
import io.mango.common.result.Require;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CalendarLunarServiceImpl implements ICalendarLunarService {

    @Override
    public LunarDayInfoVO getLunarDay(SolarDateQuery query) {
        Require.notNull(query, "日期不能为空");
        return lunarDay(query.getDate());
    }

    @Override
    public LocalDate lunarToSolar(LunarDateQuery query) {
        Require.notNull(query, "农历日期不能为空");
        int month = Boolean.TRUE.equals(query.getLeapMonth()) ? -query.getLunarMonth() : query.getLunarMonth();
        Solar solar = Lunar.fromYmd(query.getLunarYear(), month, query.getLunarDay()).getSolar();
        return LocalDate.of(solar.getYear(), solar.getMonth(), solar.getDay());
    }

    @Override
    public List<SolarTermVO> listSolarTerms(SolarTermYearQuery query) {
        Require.notNull(query, "年度不能为空");
        LocalDate yearStart = LocalDate.of(query.getYear(), 1, 1);
        LocalDate nextYearStart = yearStart.plusYears(1);
        Map<String, Solar> table = Solar.fromYmd(query.getYear(), 7, 1).getLunar().getJieQiTable();
        return table.entrySet().stream()
                .filter(entry -> isCanonicalSolarTerm(entry.getKey()))
                .map(entry -> toSolarTermVO(entry.getKey(), entry.getValue()))
                .filter(term -> !term.getDate().isBefore(yearStart) && term.getDate().isBefore(nextYearStart))
                .sorted(Comparator.comparing(SolarTermVO::getDate))
                .toList();
    }

    @Override
    public void applyLunarInfo(CalendarDay day) {
        Require.notNull(day, "日历日期不能为空");
        LunarDayInfoVO info = lunarDay(day.getCalendarDate());
        day.setLunarYear(info.getLunarYear());
        day.setLunarMonth(info.getLunarMonth());
        day.setLunarDay(info.getLunarDay());
        day.setLunarLeapMonth(info.isLunarLeapMonth() ? 1 : 0);
        day.setLunarText(info.getLunarText());
        day.setGanzhiYear(info.getGanzhiYear());
        day.setZodiac(info.getZodiac());
        day.setSolarTerm(info.getSolarTerm());
    }

    private LunarDayInfoVO lunarDay(LocalDate date) {
        Require.notNull(date, "日期不能为空");
        Solar solar = Solar.fromYmd(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        Lunar lunar = solar.getLunar();
        LunarDayInfoVO vo = new LunarDayInfoVO();
        vo.setSolarDate(date);
        vo.setLunarYear(lunar.getYear());
        vo.setLunarMonth(Math.abs(lunar.getMonth()));
        vo.setLunarDay(lunar.getDay());
        vo.setLunarLeapMonth(lunar.getMonth() < 0);
        vo.setLunarText((lunar.getMonth() < 0 ? "闰" : "") + lunar.getMonthInChinese() + "月" + lunar.getDayInChinese());
        vo.setGanzhiYear(lunar.getYearInGanZhi());
        vo.setZodiac(lunar.getYearShengXiao());
        vo.setSolarTerm(blankToNull(lunar.getJieQi()));
        return vo;
    }

    private SolarTermVO toSolarTermVO(String name, Solar solar) {
        SolarTermVO vo = new SolarTermVO();
        vo.setName(name);
        vo.setDate(LocalDate.of(solar.getYear(), solar.getMonth(), solar.getDay()));
        return vo;
    }

    private boolean isCanonicalSolarTerm(String name) {
        for (String item : Lunar.JIE_QI_IN_USE) {
            if (item.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
