package io.mango.information.starter.security;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.starter.config.InformationSubmitProtectionProperties;
import io.mango.infra.kv.api.IKvStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InformationSubmitProtectionTest {

    @Test
    void normalRequestIsAllowedBeforeRiskThreshold() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(1L);
        InformationSubmitProtection protection = protection(kvStore, null, properties(3, 10));

        InformationSubmitProtection.Decision decision = protection.check(request(), "feedback");

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void riskRequestRequiresCaptchaBeforeSubmission() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(3L);
        InformationSubmitProtection protection = protection(kvStore, null, properties(3, 10));

        InformationSubmitProtection.Decision decision = protection.check(request(), "feedback");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(428);
        assertThat(decision.code()).isEqualTo(InformationCode.SUBMISSION_CAPTCHA_REQUIRED);
    }

    @Test
    void validCaptchaAllowsRiskRequest() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(3L);
        CaptchaApi captchaApi = mock(CaptchaApi.class);
        when(captchaApi.verify(any(CaptchaVerifyRequest.class))).thenReturn(R.ok(Boolean.TRUE));
        MockHttpServletRequest request = request();
        request.addHeader(InformationSubmitProtection.HEADER_CAPTCHA_KEY, "captcha-key");
        request.addHeader(InformationSubmitProtection.HEADER_CAPTCHA_CODE, "7");
        InformationSubmitProtection protection = protection(kvStore, captchaApi, properties(3, 10));

        InformationSubmitProtection.Decision decision = protection.check(request, "feedback");

        assertThat(decision.allowed()).isTrue();
        verify(captchaApi).verify(any(CaptchaVerifyRequest.class));
    }

    @Test
    void requestOverHardLimitIsRejectedWithoutCaptchaVerification() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenReturn(11L);
        CaptchaApi captchaApi = mock(CaptchaApi.class);
        InformationSubmitProtectionProperties properties = properties(3, 10);
        properties.setMaxRequests(10);
        InformationSubmitProtection protection = protection(kvStore, captchaApi, properties);

        InformationSubmitProtection.Decision decision = protection.check(request(), "feedback");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(429);
        verify(captchaApi, org.mockito.Mockito.never()).verify(any(CaptchaVerifyRequest.class));
    }

    @Test
    void kvFailureCanBeConfiguredFailClosed() {
        IKvStore kvStore = mock(IKvStore.class);
        when(kvStore.increment(any(), any(Long.class))).thenThrow(new IllegalStateException("kv down"));
        InformationSubmitProtectionProperties properties = properties(3, 10);
        properties.setFailOpen(false);
        InformationSubmitProtection protection = protection(kvStore, null, properties);

        InformationSubmitProtection.Decision decision = protection.check(request(), "feedback");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(503);
    }

    private InformationSubmitProtection protection(IKvStore kvStore, CaptchaApi captchaApi,
            InformationSubmitProtectionProperties properties) {
        ObjectProvider<IKvStore> kvProvider = provider(kvStore);
        ObjectProvider<CaptchaApi> captchaProvider = provider(captchaApi);
        return new InformationSubmitProtection(kvProvider, captchaProvider, properties);
    }

    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private InformationSubmitProtectionProperties properties(int threshold, int maxRequests) {
        InformationSubmitProtectionProperties properties = new InformationSubmitProtectionProperties();
        properties.setRiskThreshold(threshold);
        properties.setMaxRequests(maxRequests);
        return properties;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
