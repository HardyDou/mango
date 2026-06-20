package io.mango.infra.feign.starter;

import feign.RequestTemplate;
import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternalCallFeignInterceptorTest {

    @Test
    void interceptors_applyModuleTargetBeforeInternalSignature() {
        List<RequestInterceptor> interceptors = new ArrayList<>();
        InternalCallFeignInterceptor internalCallInterceptor = new InternalCallFeignInterceptor();
        ModuleTargetFeignInterceptor moduleTargetInterceptor = new ModuleTargetFeignInterceptor(moduleName ->
                java.util.Optional.empty());
        interceptors.add(internalCallInterceptor);
        interceptors.add(moduleTargetInterceptor);

        AnnotationAwareOrderComparator.sort(interceptors);

        assertThat(interceptors).containsExactly(moduleTargetInterceptor, internalCallInterceptor);
    }

    @Test
    void apply_whenTemplateUrlIsAbsolute_signsRequestPathOnly() throws Exception {
        InternalCallFeignInterceptor interceptor = new InternalCallFeignInterceptor();
        ReflectionTestUtils.setField(interceptor, "sharedSecret", "test-secret");

        RequestTemplate template = new RequestTemplate()
                .method("POST")
                .target("http://mango-resource-capability-app")
                .uri("/resource/declarations/register");
        template.query("b", "2");
        template.query("a", "1");

        interceptor.apply(template);

        String timestamp = firstHeader(template, "X-Internal-Timestamp");
        String nonce = firstHeader(template, "X-Internal-Nonce");
        String signature = firstHeader(template, "X-Internal-Signature");
        String payload = timestamp + ":" + nonce + ":POST:/resource/declarations/register:a=1&b=2";

        assertThat(firstHeader(template, "X-Internal-Call")).isEqualTo("true");
        assertThat(signature).isEqualTo(hmacSha256(payload, "test-secret"));
    }

    private String firstHeader(RequestTemplate template, String name) {
        return template.headers().get(name).iterator().next();
    }

    private String hmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hmacBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
