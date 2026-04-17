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
                Optional.of(new ModuleInfo(moduleName, "mango-admin-app", "/admin", "test")));
        RequestTemplate template = new RequestTemplate()
                .feignTarget(new Target.HardCodedTarget<>(Object.class, "mango-rbac", "http://mango-rbac"))
                .uri("/public-path/check");

        interceptor.apply(template);

        assertThat(template.url()).isEqualTo("http://mango-admin-app/admin/public-path/check");
    }
}
