package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceAuditAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void auditHandler_shouldFillAuditFieldsFromMangoContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "2002", "admin", "platform", "employee", "org", 3003L, "admin-app"));

        contextRunner.run(ctx -> {
            MetaObjectHandler handler = ctx.getBean(MetaObjectHandler.class);
            TestEntity entity = new TestEntity();
            MetaObject metaObject = SystemMetaObject.forObject(entity);

            handler.insertFill(metaObject);

            assertThat(entity.getCreatedBy()).isEqualTo(1001L);
            assertThat(entity.getUpdatedBy()).isEqualTo(1001L);
            assertThat(entity.getTenantId()).isEqualTo("2002");
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getCreateTime()).isNotNull();
            assertThat(entity.getUpdateTime()).isNotNull();
        });
    }

    @Test
    void auditHandler_shouldOverrideTenantIdFromContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "2002", "admin", "platform", "employee", "org", 3003L, "admin-app"));

        contextRunner.run(ctx -> {
            MetaObjectHandler handler = ctx.getBean(MetaObjectHandler.class);
            TestEntity entity = new TestEntity();
            entity.setTenantId("9999");
            MetaObject metaObject = SystemMetaObject.forObject(entity);

            handler.insertFill(metaObject);

            assertThat(entity.getTenantId()).isEqualTo("2002");
        });
    }

    @Test
    void customContextProvider_shouldBeUsed() {
        contextRunner
                .withUserConfiguration(CustomContextProviderConfig.class)
                .run(ctx -> assertThat(ctx.getBean(PersistenceContextProvider.class))
                        .isSameAs(ctx.getBean("customProvider")));
    }

    @Test
    void disabledAudit_shouldNotCreateMetaObjectHandler() {
        contextRunner
                .withPropertyValues("mango.persistence.audit.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MetaObjectHandler.class));
    }

    @Configuration
    static class CustomContextProviderConfig {
        @Bean
        PersistenceContextProvider customProvider() {
            return io.mango.infra.persistence.api.context.PersistenceContext::empty;
        }
    }

    static class TestEntity {
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime createTime;
        private Long updatedBy;
        private LocalDateTime updatedAt;
        private LocalDateTime updateTime;
        private String tenantId;

        public Long getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Long createdBy) {
            this.createdBy = createdBy;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public Long getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
