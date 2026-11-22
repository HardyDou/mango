package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.authorization.api.annotation.DataGrant;
import io.mango.common.result.R;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 证明 {@code ResponseBodyAdvice#beforeBodyWrite} 中可以从请求属性取到 {@link HandlerMethod}。
 *
 * <p>该能力是响应补签 Grant 的前提：{@code beforeBodyWrite} 没有 HandlerMethod 参数，只能通过
 * {@code HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE} 反查。</p>
 */
class DataGrantHandlerMethodAttributeTest {

    @Test
    void resolveHandlerMethodReturnsMatchedHandler() throws Exception {
        CapturingAdvice advice = new CapturingAdvice();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(advice)
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/probe/granted"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        HandlerMethod handlerMethod = advice.captured();
        assertThat(handlerMethod).isNotNull();
        assertThat(handlerMethod.getMethod().getName()).isEqualTo("granted");
        DataGrant dataGrant = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                DataGrant.class);
        assertThat(dataGrant).isNotNull();
        assertThat(dataGrant.domain()).isEqualTo("probe:item");
    }

    @Test
    void resolveHandlerMethodIsAvailableWithoutAnnotation() throws Exception {
        CapturingAdvice advice = new CapturingAdvice();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(advice)
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/probe/plain"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(advice.captured()).isNotNull();
        assertThat(advice.captured().getMethod().getName()).isEqualTo("plain");
    }

    /**
     * 探针控制器。
     */
    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/granted")
        @DataGrant(domain = "probe:item")
        R<String> granted() {
            return R.ok("ok");
        }

        @GetMapping("/plain")
        R<String> plain() {
            return R.ok("ok");
        }
    }

    /**
     * 捕获 HandlerMethod 的响应通知器。
     */
    @org.springframework.web.bind.annotation.ControllerAdvice
    private static final class CapturingAdvice implements ResponseBodyAdvice<Object> {

        private final AtomicReference<HandlerMethod> captured = new AtomicReference<>();

        HandlerMethod captured() {
            return captured.get();
        }

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
                ServerHttpResponse response) {
            captured.set(DataGrantResponseAdvice.resolveHandlerMethod(request));
            return body;
        }
    }
}
