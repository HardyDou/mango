package io.mango.infra.feign.starter;

import feign.RequestTemplate;
import feign.Target;
import io.mango.infra.module.api.ModuleInfo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleTargetFeignInterceptorTest {

    @Test
    void apply_whenModuleInfoExists_rewritesTargetAndPath() {
        ModuleTargetFeignInterceptor interceptor = new ModuleTargetFeignInterceptor(moduleName ->
                Optional.of(new ModuleInfo(moduleName, "mango-admin-app", "/admin", "/rbac", "test")));
        RequestTemplate template = new RequestTemplate()
                .feignTarget(new Target.HardCodedTarget<>(
                        Object.class, "mango-authorization", "http://mango-authorization"))
                .uri("/api-resources/access-decision");

        interceptor.apply(template);

        assertThat(template.url()).isEqualTo("http://mango-admin-app/admin/api-resources/access-decision");
    }

    @Test
    void apply_whenTemplateUrlIsAbsolute_keepsExplicitDynamicTarget() {
        ModuleTargetFeignInterceptor interceptor = new ModuleTargetFeignInterceptor(moduleName ->
                Optional.of(new ModuleInfo(moduleName, "mango-resource-capability-app", "", "/resource", "test")));
        RequestTemplate template = new RequestTemplate()
                .feignTarget(new Target.HardCodedTarget<>(
                        Object.class, "mango-resource", "http://mango-resource"))
                .target("http://mango-authorization-capability-app")
                .uri("/_resource/targets/upsert-batch");

        interceptor.apply(template);

        assertThat(template.url())
                .isEqualTo("http://mango-authorization-capability-app/_resource/targets/upsert-batch");
    }
}
