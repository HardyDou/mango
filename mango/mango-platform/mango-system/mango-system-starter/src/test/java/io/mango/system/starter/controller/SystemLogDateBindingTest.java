package io.mango.system.starter.controller;

import io.mango.common.vo.PageResult;
import io.mango.infra.web.starter.MangoWebProperties;
import io.mango.infra.web.starter.WebAutoConfiguration;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.core.service.ISysLogService;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemLogDateBindingTest {

    @Test
    void loginLogDateOnlyQueryBindsToLocalDateTime() throws Exception {
        ISysLogService service = mock(ISysLogService.class);
        LoginLogPageQuery[] captured = new LoginLogPageQuery[1];
        when(service.pageLoginLogs(any())).thenAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            return PageResult.of(List.of(), 0, 1, 10);
        });
        MockMvc mvc = mockMvc(new SysLoginLogController(service));

        mvc.perform(get("/system/log/login/list")
                        .param("startTime", "2026-08-27")
                        .param("endTime", "2026-08-27"))
                .andExpect(status().isOk());

        assertThat(captured[0].getStartTime()).isEqualTo(LocalDateTime.of(2026, 8, 27, 0, 0));
        assertThat(captured[0].getEndTime()).isEqualTo(LocalDateTime.of(2026, 8, 27, 0, 0));
    }

    @Test
    void operationLogDateTimeQueryKeepsTimeComponents() throws Exception {
        ISysLogService service = mock(ISysLogService.class);
        OperationLogPageQuery[] captured = new OperationLogPageQuery[1];
        when(service.pageOperationLogs(any())).thenAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            return PageResult.of(List.of(), 0, 1, 10);
        });
        MockMvc mvc = mockMvc(new SysOperationLogController(service));

        mvc.perform(get("/system/log/operation/list")
                        .param("startTime", "2026-08-27 08:00:00")
                        .param("endTime", "2026-08-27 14:00:00"))
                .andExpect(status().isOk());

        assertThat(captured[0].getStartTime()).isEqualTo(LocalDateTime.of(2026, 8, 27, 8, 0));
        assertThat(captured[0].getEndTime()).isEqualTo(LocalDateTime.of(2026, 8, 27, 14, 0));
    }

    private MockMvc mockMvc(Object controller) {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        new WebAutoConfiguration(new MangoWebProperties()).addFormatters(conversionService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(conversionService)
                .build();
    }
}
