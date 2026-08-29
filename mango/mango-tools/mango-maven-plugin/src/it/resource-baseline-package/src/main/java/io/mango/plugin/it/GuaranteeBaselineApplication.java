package io.mango.plugin.it;

import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.bootstrap.starter.MangoApplication;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.core.capability.KvStoreLeaseLocker;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.resource.support.ResourceBaselinePolicy;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class GuaranteeBaselineApplication {

    private static final String EXTERNAL_ASSET =
            "META-INF/mango/assets/guarantee-baseline/external-payload.txt";
    private static final String RESOURCE_BASELINE_BUILD_ARGUMENT =
            "--mango.bootstrap.resource-baseline-build-enabled=true";

    public static void main(String[] args) {
        boolean resourceBaselineBuild = Arrays.asList(args).contains(RESOURCE_BASELINE_BUILD_ARGUMENT);
        if (resourceBaselineBuild && !new ClassPathResource(EXTERNAL_ASSET).isReadable()) {
            throw new IllegalStateException("External Resource baseline asset is not readable: " + EXTERNAL_ASSET);
        }
        MangoApplication.run(GuaranteeBaselineApplication.class, args);
    }

    @Bean
    ResourceHandler guaranteeProductHandler(JdbcTemplate jdbcTemplate) {
        return new JdbcResourceHandler(jdbcTemplate, "GUARANTEE_PRODUCT", "guarantee_product",
                ResourceBaselinePolicy.PORTABLE);
    }

    @Bean
    ResourceHandler guaranteeEnvironmentHandler(JdbcTemplate jdbcTemplate) {
        return new JdbcResourceHandler(jdbcTemplate, "GUARANTEE_ENV_ENDPOINT", "guarantee_env_endpoint",
                ResourceBaselinePolicy.ENVIRONMENT_REQUIRED);
    }

    @Bean
    ILeaseLocker resourceLeaseLocker() {
        return new KvStoreLeaseLocker(new MemoryKvStore(1));
    }

    @Bean
    BootstrapStepContributor guaranteeBusinessBootstrap(JdbcTemplate jdbcTemplate) {
        return () -> List.of(new BootstrapStep() {
            @Override
            public String code() {
                return "GUARANTEE_BUSINESS_BOOTSTRAP";
            }

            @Override
            public BootstrapPhase phase() {
                return BootstrapPhase.EXPAND;
            }

            @Override
            public String fingerprintMaterial() {
                return "guarantee-business-bootstrap-v1";
            }

            @Override
            public BootstrapStepResult execute(BootstrapExecutionContext context) {
                jdbcTemplate.update("INSERT INTO guarantee_business_bootstrap (id, marker) VALUES (1, ?) "
                        + "ON DUPLICATE KEY UPDATE marker = VALUES(marker)", "runtime-only");
                return new BootstrapStepResult("Guarantee business Bootstrap completed", Map.of("rows", 1));
            }
        });
    }

    private record JdbcResourceHandler(
            JdbcTemplate jdbcTemplate,
            String resourceType,
            String table,
            ResourceBaselinePolicy baselinePolicy) implements ResourceHandler {

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            long id = Long.parseLong(resource.getId());
            String value = String.valueOf(resource.getFields().get("value").getValue());
            jdbcTemplate.update("INSERT INTO " + table + " (id, biz_code, managed_value) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE managed_value = VALUES(managed_value)",
                    id, resource.getBizKey(), value);
            return ResourceSyncResult.of(id, table, resourceType + " synchronized");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            jdbcTemplate.update("UPDATE " + table + " SET enabled = 0 WHERE id = ?",
                    Long.parseLong(resource.getId()));
            return ResourceSyncResult.of(Long.parseLong(resource.getId()), table,
                    resourceType + " disabled");
        }
    }
}
