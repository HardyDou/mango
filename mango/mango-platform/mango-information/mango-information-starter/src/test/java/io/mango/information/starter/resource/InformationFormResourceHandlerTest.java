package io.mango.information.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.information.api.enums.InformationFormStatus;
import io.mango.information.core.entity.InformationFormEntity;
import io.mango.information.core.mapper.InformationFormMapper;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 内置表单资源同步测试：使用真实 Mapper 与内存库验证幂等、状态与删除语义。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PersistenceAuditAutoConfiguration.class,
        InformationFormResourceHandlerTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:information-resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class InformationFormResourceHandlerTest {

    private static final String DEFINITION = """
            [{"type":"input","field":"name","title":"姓名","validate":[{"required":true}]},
             {"type":"rate","field":"score","title":"评分"}]
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InformationFormResourceHandler handler;

    @Autowired
    private InformationFormMapper formMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS information_submission_snapshot");
        jdbcTemplate.execute("DROP TABLE IF EXISTS information_submission");
        jdbcTemplate.execute("DROP TABLE IF EXISTS information_form");
        jdbcTemplate.execute("""
                CREATE TABLE information_form (
                  id bigint NOT NULL,
                  tenant_id varchar(64) NOT NULL,
                  org_id bigint DEFAULT NULL,
                  form_code varchar(64) NOT NULL,
                  form_name varchar(128) NOT NULL,
                  description varchar(1000) DEFAULT NULL,
                  definition_json text NOT NULL,
                  success_message varchar(255) DEFAULT NULL,
                  status varchar(32) NOT NULL DEFAULT 'DRAFT',
                  server_secret varchar(128) NOT NULL,
                  server_secret_digest varchar(128) DEFAULT NULL,
                  created_by bigint DEFAULT NULL,
                  created_at datetime DEFAULT NULL,
                  updated_by bigint DEFAULT NULL,
                  updated_at datetime DEFAULT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_information_form_tenant_code (tenant_id, form_code)
                )
                """);
    }

    @Test
    void upsert_createsBuiltInFormAndIsIdempotent() {
        ResourceSyncResult created = handler.upsert(declaration("1001", "builtin-feedback", "内置反馈表", "PUBLISHED"));
        assertThat(created.getTargetId()).isEqualTo(1001L);

        InformationFormEntity stored = entityByCode("builtin-feedback");
        assertThat(stored.getFormName()).isEqualTo("内置反馈表");
        assertThat(stored.getStatus()).isEqualTo(InformationFormStatus.PUBLISHED.name());
        assertThat(stored.getTenantId()).isEqualTo("1");
        assertThat(stored.getServerSecret()).isNotBlank();
        // 资源路径创建的表单同样要有 SK 摘要，列不可为空
        assertThat(stored.getServerSecretDigest()).isNotBlank();
        assertThat(stored.getSuccessMessage()).isEqualTo("提交成功");

        // 同一 formCode 再次同步只更新定义，不产生第二条表单
        handler.upsert(declaration("1001", "builtin-feedback", "内置反馈表 V2", "DISABLED"));
        assertThat(countByCode("builtin-feedback")).isEqualTo(1);
        InformationFormEntity updated = entityByCode("builtin-feedback");
        assertThat(updated.getFormName()).isEqualTo("内置反馈表 V2");
        assertThat(updated.getStatus()).isEqualTo(InformationFormStatus.DISABLED.name());
        assertThat(updated.getServerSecret()).isEqualTo(stored.getServerSecret());
    }

    @Test
    void upsert_acceptsDefaultsAndRejectsInvalidDeclaration() {
        handler.upsert(declaration(null, "builtin-default", "默认状态表", null));
        InformationFormEntity stored = entityByCode("builtin-default");
        assertThat(stored.getStatus()).isEqualTo(InformationFormStatus.PUBLISHED.name());
        assertThat(stored.getId()).isNotNull();

        Map<String, String> withoutDefinition = new LinkedHashMap<>();
        withoutDefinition.put("formCode", "builtin-bad");
        withoutDefinition.put("formName", "缺定义");
        assertThatThrownBy(() -> handler.upsert(declaration(withoutDefinition)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("definitionJson");

        Map<String, String> invalid = new LinkedHashMap<>();
        invalid.put("formCode", "builtin-bad-2");
        invalid.put("formName", "非法定义");
        invalid.put("definitionJson", "[]");
        assertThatThrownBy(() -> handler.upsert(declaration(invalid)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("至少需要一个可提交采集项");
    }

    @Test
    void disableAndDelete_updateResourceLifecycle() {
        handler.upsert(declaration("1003", "builtin-lifecycle", "生命周期表", "PUBLISHED"));
        handler.disable(declaration("1003", "builtin-lifecycle", "生命周期表", "PUBLISHED"));
        assertThat(entityByCode("builtin-lifecycle").getStatus()).isEqualTo(InformationFormStatus.DISABLED.name());

        handler.delete(declaration("1003", "builtin-lifecycle", "生命周期表", "PUBLISHED"));
        assertThat(countByCode("builtin-lifecycle")).isZero();
    }

    @Test
    void spec_declaresFormFields() {
        assertThat(handler.resourceType()).isEqualTo(InformationResourceTypes.FORM);
        assertThat(handler.executionTenantField()).isEqualTo("tenantId");
        assertThat(handler.spec().getRequiredFields())
                .contains("formCode", "formName", "definitionJson");
        assertThat(handler.spec().getFieldDescriptions())
                .containsKeys("status", "successMessage", "targetId");
    }

    private ResourceDeclaration declaration(String targetId, String formCode, String formName, String status) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("formCode", formCode);
        values.put("formName", formName);
        values.put("definitionJson", DEFINITION);
        if (targetId != null) {
            values.put("targetId", targetId);
        }
        if (status != null) {
            values.put("status", status);
        }
        return declaration(values);
    }

    private ResourceDeclaration declaration(Map<String, String> values) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setResourceType(InformationResourceTypes.FORM);
        // ResourceDeclaration.getFields() 返回副本，必须用 putField 写入
        values.forEach((key, value) -> {
            ResourceField field = new ResourceField();
            field.setType(ResourceFieldType.STRING);
            field.setValue(value);
            declaration.putField(key, field);
        });
        return declaration;
    }

    private InformationFormEntity entityByCode(String formCode) {
        return formMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InformationFormEntity>()
                .eq(InformationFormEntity::getTenantId, "1")
                .eq(InformationFormEntity::getFormCode, formCode)
                .last("LIMIT 1"));
    }

    private int countByCode(String formCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_form WHERE tenant_id = '1' AND form_code = ?", Integer.class, formCode);
        return count == null ? 0 : count;
    }

    @Configuration
    // 只装配资源处理器所需依赖，避免拉起文件、身份等无关能力
    @ComponentScan(basePackages = "io.mango.information.starter.resource")
    @org.mybatis.spring.annotation.MapperScan(basePackages = "io.mango.information.core.mapper")
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
