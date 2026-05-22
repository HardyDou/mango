package io.mango.calendar.starter.remote;

import io.mango.calendar.api.CalendarApi;
import io.mango.calendar.api.query.AddWorkdaysQuery;
import io.mango.calendar.api.query.BatchCheckWorkdayQuery;
import io.mango.calendar.api.query.CalendarDateQuery;
import io.mango.calendar.api.query.CountWorkdaysQuery;
import io.mango.calendar.api.query.DateRangeQuery;
import io.mango.calendar.api.query.MonthQuery;
import io.mango.calendar.api.query.NthWorkdayOfMonthQuery;
import io.mango.calendar.api.vo.CalendarDayVO;
import io.mango.calendar.api.vo.MonthWorkdaySummaryVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "mango-calendar", path = "/calendar")
public interface CalendarFeignClient extends CalendarApi {

    @Override
    @GetMapping("/workdays/day")
    R<CalendarDayVO> getDay(@SpringQueryMap CalendarDateQuery query);

    @Override
    @GetMapping("/workdays/check")
    R<Boolean> isWorkday(@SpringQueryMap CalendarDateQuery query);

    @Override
    @GetMapping("/workdays/next")
    R<LocalDate> nextWorkday(@SpringQueryMap CalendarDateQuery query);

    @Override
    @GetMapping("/workdays/previous")
    R<LocalDate> previousWorkday(@SpringQueryMap CalendarDateQuery query);

    @Override
    @GetMapping("/workdays/add")
    R<LocalDate> addWorkdays(@SpringQueryMap AddWorkdaysQuery query);

    @Override
    @GetMapping("/workdays/count")
    R<Integer> countWorkdays(@SpringQueryMap CountWorkdaysQuery query);

    @Override
    @GetMapping("/workdays/list")
    R<List<CalendarDayVO>> listDays(@SpringQueryMap DateRangeQuery query);

    @Override
    @PostMapping("/workdays/batch-check")
    R<List<CalendarDayVO>> batchCheck(@RequestBody BatchCheckWorkdayQuery query);

    @Override
    @GetMapping("/workdays/month/summary")
    R<MonthWorkdaySummaryVO> monthSummary(@SpringQueryMap MonthQuery query);

    @Override
    @GetMapping("/workdays/month/first")
    R<LocalDate> firstWorkdayOfMonth(@SpringQueryMap MonthQuery query);

    @Override
    @GetMapping("/workdays/month/last")
    R<LocalDate> lastWorkdayOfMonth(@SpringQueryMap MonthQuery query);

    @Override
    @GetMapping("/workdays/month/nth")
    R<LocalDate> nthWorkdayOfMonth(@SpringQueryMap NthWorkdayOfMonthQuery query);
}
