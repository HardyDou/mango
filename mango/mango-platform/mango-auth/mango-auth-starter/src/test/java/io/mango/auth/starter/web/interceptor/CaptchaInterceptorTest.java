package io.mango.auth.starter.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.spi.CaptchaConfigService;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.infra.context.api.MangoContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaInterceptorTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void successfulUnifiedResponseAllowsRequest() throws Exception {
        CaptchaApiStub captchaApi = new CaptchaApiStub(R.ok(Boolean.TRUE));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor(captchaApi).preHandle(request(), response, new Object());

        assertThat(allowed).isTrue();
        assertThat(captchaApi.lastVerifyRequest.getKey()).isEqualTo("captcha-key");
        assertThat(captchaApi.lastVerifyRequest.getCode()).isEqualTo("7");
        assertThat(captchaApi.lastVerifyRequest.getType().name()).isEqualTo("ARITHMETIC");
    }

    @Test
    void unsuccessfulUnifiedResponseRejectsRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor(new CaptchaApiStub(R.fail(500, "remote failure")))
                .preHandle(request(), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString())
                .contains("\"success\":false", String.valueOf(AuthCode.CAPTCHA_INVALID.getCode()));
    }

    @Test
    void falseDataRejectsRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor(new CaptchaApiStub(R.ok(Boolean.FALSE)))
                .preHandle(request(), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void captchaBusinessExceptionRejectsRequest() throws Exception {
        CaptchaApiStub captchaApi = new CaptchaApiStub(null);
        captchaApi.failure = new BizException(AuthCode.CAPTCHA_INVALID.getCode(), "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor(captchaApi).preHandle(request(), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    private static CaptchaInterceptor interceptor(CaptchaApi captchaApi) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("captchaApi", captchaApi);
        return new CaptchaInterceptor(
                new RequiredCaptchaConfig(),
                beanFactory.getBeanProvider(CaptchaApi.class),
                new ObjectMapper());
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Captcha-Key", "captcha-key");
        request.addHeader("X-Captcha-Code", "7");
        request.addHeader("X-Captcha-Type", "ARITHMETIC");
        return request;
    }

    private static final class RequiredCaptchaConfig implements CaptchaConfigService {

        @Override
        public boolean isCaptchaRequired(String path) {
            return true;
        }

        @Override
        public String getCaptchaType(String path) {
            return "ARITHMETIC";
        }

        @Override
        public long getCaptchaTtl(String path) {
            return 300L;
        }
    }

    private static final class CaptchaApiStub implements CaptchaApi {

        private final R<Boolean> verifyResult;
        private CaptchaVerifyRequest lastVerifyRequest;
        private BizException failure;

        private CaptchaApiStub(R<Boolean> verifyResult) {
            this.verifyResult = verifyResult;
        }

        @Override
        public R<CaptchaTypesResponse> getTypes() {
            return R.ok(new CaptchaTypesResponse());
        }

        @Override
        public R<CaptchaResponse> generateArithmetic() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateBlockPuzzle() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateClickWord() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateBehavior() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<BehaviorCaptchaVerifyResponse> verifyBehavior(CaptchaVerifyRequest request) {
            return R.ok(new BehaviorCaptchaVerifyResponse());
        }

        @Override
        public R<Boolean> verify(CaptchaVerifyRequest request) {
            lastVerifyRequest = request;
            if (failure != null) {
                throw failure;
            }
            return verifyResult;
        }

        @Override
        public R<String> send(CaptchaSendRequest request) {
            return R.ok("captcha-key");
        }
    }
}
