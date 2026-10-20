package io.mango.information.starter.controller;

import io.mango.captcha.api.CaptchaApi;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.core.service.IInformationFormService;
import io.mango.information.core.service.IInformationSubmissionService;
import io.mango.information.starter.config.InformationSubmitProtectionProperties;
import io.mango.information.starter.security.InformationSubmitProtection;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InformationPublicControllerTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rateLimitedHttpRequestReturnsRetryAfterAndDoesNotCallSubmissionService() throws Exception {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(31L);
        InformationSubmitProtection protection = new InformationSubmitProtection(
                provider(kvStore), provider((CaptchaApi) null), new InformationSubmitProtectionProperties());
        IInformationSubmissionService submissionService = mock(IInformationSubmissionService.class);
        InformationPublicController controller = new InformationPublicController(
                mock(IInformationFormService.class), submissionService, protection);

        MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(controller).build();
        mockMvc.perform(post("/information/open/forms/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formCode\":\"feedback\",\"values\":{\"name\":\"visitor\"}}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));

        verify(submissionService, never()).submit(any(), any(Boolean.class));
    }

    @Test
    void rateLimitedRequestDoesNotCallSubmissionService() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(31L);
        InformationSubmitProtectionProperties properties = new InformationSubmitProtectionProperties();
        InformationSubmitProtection protection = new InformationSubmitProtection(
                provider(kvStore), provider((CaptchaApi) null), properties);
        IInformationSubmissionService submissionService = mock(IInformationSubmissionService.class);
        InformationPublicController controller = new InformationPublicController(
                mock(IInformationFormService.class), submissionService, protection);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        SubmitInformationCommand command = new SubmitInformationCommand();
        command.setFormCode("feedback");
        command.setValues(java.util.Map.of("name", "visitor"));
        var result = controller.submit(command);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(result.isSuccess()).isFalse();
        verify(submissionService, never()).submit(any(), any(Boolean.class));
    }

    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
