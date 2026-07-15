package io.mango.calendar.starter.controller;

import io.mango.calendar.core.service.ICalendarAdminService;
import io.mango.calendar.core.service.ICalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({CalendarController.class, CalendarAdminController.class, CalendarValidationExceptionHandler.class})
class CalendarControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICalendarService calendarService;

    @MockBean
    private ICalendarAdminService calendarAdminService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    void addWorkdays_getQuery_keepsHttpContract() throws Exception {
        when(calendarService.addWorkdays(argThat(query ->
                "CN_STANDARD".equals(query.getCalendarCode())
                        && LocalDate.of(2026, 1, 1).equals(query.getSourceDate())
                        && query.getAmount() == 2)))
                .thenReturn(LocalDate.of(2026, 1, 5));

        mockMvc.perform(get("/calendar/workdays/add")
                        .param("calendarCode", "CN_STANDARD")
                        .param("sourceDate", "2026-01-01")
                        .param("amount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("2026-01-05"));
    }

    @Test
    void batchCheck_postBody_keepsJsonContract() throws Exception {
        when(calendarService.batchCheck(argThat(request ->
                "CN_STANDARD".equals(request.getCalendarCode())
                        && request.getDates().equals(List.of(LocalDate.of(2026, 1, 1))))))
                .thenReturn(List.of());

        mockMvc.perform(post("/calendar/workdays/batch-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "calendarCode": "CN_STANDARD",
                                  "dates": ["2026-01-01"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createCalendar_invalidBody_isRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/calendar/admin/calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarCode\":\"\",\"calendarName\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(calendarAdminService, never()).createCalendar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteCalendar_nonPositiveId_isRejectedBeforeService() throws Exception {
        mockMvc.perform(delete("/calendar/admin/calendars").param("id", "0"))
                .andExpect(status().isBadRequest());

        verify(calendarAdminService, never()).deleteCalendar(org.mockito.ArgumentMatchers.anyLong());
    }
}
