package io.mango.information.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.information.api.command.ProcessInformationSubmissionCommand;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.enums.InformationFormStatus;
import io.mango.information.api.enums.InformationSubmissionStatus;
import io.mango.information.api.command.UpdateInformationFormStatusCommand;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.query.InformationSubmissionPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import io.mango.information.core.service.IInformationFormService;
import io.mango.information.core.service.IInformationSubmissionService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 信息采集真实持久化链路集成测试：使用 MySQL 兼容模式的内存库覆盖字段校验、快照、幂等、SK、状态、导出与租户隔离。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PersistenceAuditAutoConfiguration.class,
        InformationServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=${information.test.datasource.url:jdbc:h2:mem:information;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "spring.datasource.username=${information.test.datasource.username:sa}",
        "spring.datasource.password=${information.test.datasource.password:}",
        "spring.datasource.driver-class-name=${information.test.datasource.driver-class-name:org.h2.Driver}",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class InformationServiceIntegrationTest {

    private static final String DEFINITION = """
            [{"fieldCode":"name","fieldName":"姓名","fieldType":"text","required":true,"sort":1},
             {"fieldCode":"phone","fieldName":"手机号","fieldType":"phone","required":true,"sort":2}]
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IInformationFormService formService;

    @Autowired
    private IInformationSubmissionService submissionService;

    @BeforeEach
    void setUp() {
        rebuildSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void saveForm_listsByTenantAndRejectsDuplicateCode() {
        Long formId = createPublishedForm("tenant-one-form");

        assertThat(formService.page(new InformationFormPageQuery()).getList())
                .singleElement()
                .satisfies(vo -> assertThat(vo.getFormCode()).isEqualTo("tenant-one-form"));

        InformationFormDetailVO detail = formService.detail(formId);
        assertThat(detail.getServerSecret()).isNotBlank();
        assertThat(detail.getHostedPath()).contains("tenant-one-form");

        SaveInformationFormCommand duplicate = new SaveInformationFormCommand();
        duplicate.setFormCode("tenant-one-form");
        duplicate.setFormName("重复表单");
        duplicate.setDefinitionJson(DEFINITION);
        assertThatThrownBy(() -> formService.save(duplicate))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void submit_writesSnapshotAndHonoursIdempotencyKey() {
        createPublishedForm("contact-form");

        InformationSubmissionResultVO first = submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-1"), false);
        InformationSubmissionResultVO repeated = submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-1"), false);
        InformationSubmissionResultVO second = submissionService.submit(
                submitCommand("contact-form", Map.of("name", "李四", "phone", "13900139000"), "key-2"), false);

        assertThat(repeated.getId()).isEqualTo(first.getId());
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(intValue("information_submission", "count(*)")).isEqualTo(2);
        assertThat(intValue("information_submission_snapshot", "count(*)")).isEqualTo(2);
        assertThat(stringValue("information_submission", "tenant_id", "id = " + first.getId())).isEqualTo("1");
        assertThat(submissionService.page(new InformationSubmissionPageQuery()).getList())
                .hasSize(2)
                .allSatisfy(vo -> assertThat(vo.getDefinitionJson()).contains("phone"));
    }

    @Test
    void submit_rejectsMissingRequiredAndUnknownFields() {
        createPublishedForm("contact-form");

        assertThatThrownBy(() -> submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三"), "key-1"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("phone");

        assertThatThrownBy(() -> submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三", "phone", "1", "hack", "x"), "key-2"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未知字段");

        assertThat(intValue("information_submission", "count(*)")).isZero();
    }

    @Test
    void serverSubmit_requiresMatchingSecret() {
        Long formId = createPublishedForm("contact-form");
        InformationFormDetailVO detail = formService.detail(formId);

        SubmitInformationCommand invalid = submitCommand(
                "contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-1");
        invalid.setServerSecret("wrong-secret");
        assertThatThrownBy(() -> submissionService.submit(invalid, true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密钥无效");

        SubmitInformationCommand valid = submitCommand(
                "contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-2");
        valid.setServerSecret(detail.getServerSecret());
        assertThat(submissionService.submit(valid, true).getId()).isNotNull();
    }

    @Test
    void process_rejectsClosedSubmission() {
        createPublishedForm("contact-form");
        Long submissionId = submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-1"), false).getId();

        assertThat(process(submissionId, InformationSubmissionStatus.COMPLETED)).isTrue();
        assertThat(process(submissionId, InformationSubmissionStatus.CLOSED)).isTrue();
        assertThatThrownBy(() -> process(submissionId, InformationSubmissionStatus.PROCESSING))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已关闭");
    }

    @Test
    void page_filtersBySubmittedTimeRange() {
        createPublishedForm("time-range-form");
        submissionService.submit(
                submitCommand("time-range-form", Map.of("name", "时间范围", "phone", "13800138000"), "key-1"), false);

        InformationSubmissionPageQuery future = new InformationSubmissionPageQuery();
        future.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        assertThat(submissionService.page(future).getList()).isEmpty();

        InformationSubmissionPageQuery today = new InformationSubmissionPageQuery();
        today.setStartTime(java.time.LocalDateTime.now().minusDays(1));
        today.setEndTime(java.time.LocalDateTime.now().plusMinutes(5));
        assertThat(submissionService.page(today).getList()).hasSize(1);
    }

    @Test
    void export_returnsCurrentFormRowsWithMaskedPhone() {
        createPublishedForm("contact-form");
        submissionService.submit(
                submitCommand("contact-form", Map.of("name", "张三", "phone", "13800138000"), "key-1"), false);

        InformationSubmissionPageQuery query = new InformationSubmissionPageQuery();
        String csv = submissionService.exportCsv(query);

        assertThat(csv).contains("姓名").contains("手机号");
        assertThat(csv).contains("张三").contains("138****00");
        assertThat(csv).doesNotContain("13800138000");
    }

    @Test
    void tenantIsolation_keepsFormsAndSubmissionsSeparated() {
        createPublishedForm("shared-code");
        submissionService.submit(
                submitCommand("shared-code", Map.of("name", "租户一", "phone", "13800138000"), "key-1"), false);

        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("2"));
        assertThat(formService.page(new InformationFormPageQuery()).getList()).isEmpty();
        createPublishedForm("shared-code");
        submissionService.submit(
                submitCommand("shared-code", Map.of("name", "租户二", "phone", "13900139000"), "key-1"), false);

        assertThat(formService.page(new InformationFormPageQuery()).getList()).hasSize(1);
        assertThat(submissionService.page(new InformationSubmissionPageQuery()).getList())
                .singleElement()
                .satisfies(vo -> assertThat(vo.getId()).isNotNull());
        assertThat(intValue("information_submission", "count(*)", "tenant_id = '1'")).isEqualTo(1);
        assertThat(intValue("information_submission", "count(*)", "tenant_id = '2'")).isEqualTo(1);
    }

    @Test
    void saveForm_acceptsFormCreateRulesDefinition() {
        // 设计器产出的是 form-create 规则形状：{type, field, title, validate, children}
        String designerRules = """
                [{"type":"input","field":"company","title":"公司名称","props":{"placeholder":"请输入公司名称"},
                  "validate":[{"required":true,"message":"请填写公司名称"}]},
                 {"type":"select","field":"scale","title":"企业规模","options":[{"label":"小型","value":"s"},{"label":"大型","value":"l"}]},
                 {"type":"elDivider","title":"以下为展示区"},
                 {"type":"input","field":"contact","title":"联系人","children":[]}]
                """;
        SaveInformationFormCommand command = new SaveInformationFormCommand();
        command.setFormCode("designer-form");
        command.setFormName("设计器表单");
        command.setDefinitionJson(designerRules);
        command.setStatus(InformationFormStatus.PUBLISHED);
        Long formId = formService.save(command);

        assertThat(formService.page(new InformationFormPageQuery()).getList())
                .singleElement()
                .satisfies(vo -> assertThat(vo.getFieldCount()).isEqualTo(3));
        assertThat(formService.detail(formId).getSubmissionCount()).isZero();

        // 必填来自 validate 数组，展示型规则不参与校验
        assertThatThrownBy(() -> submissionService.submit(
                submitCommand("designer-form", Map.of("scale", "s"), "key-1"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("company");

        assertThat(submissionService.submit(
                submitCommand("designer-form", Map.of("company", "芒果", "scale", "l", "contact", "张三"), "key-2"), false)
                .getId()).isNotNull();

        // 展示型规则不是采集项，不接受提交值
        assertThatThrownBy(() -> submissionService.submit(
                submitCommand("designer-form", Map.of("company", "芒果", "scale", "l", "elDivider", "x"), "key-3"), false))
                .isInstanceOf(BizException.class);
    }

    @Test
    void updateStatus_andRegenerateSecret_rotateFormLifecycle() {
        Long formId = createPublishedForm("lifecycle-form");
        String before = formService.detail(formId).getServerSecret();

        UpdateInformationFormStatusCommand status = new UpdateInformationFormStatusCommand();
        status.setId(formId);
        status.setStatus(InformationFormStatus.DISABLED);
        assertThat(formService.updateStatus(status)).isTrue();
        assertThat(formService.detail(formId).getStatus()).isEqualTo(InformationFormStatus.DISABLED);
        assertThatThrownBy(() -> submissionService.submit(
                submitCommand("lifecycle-form", Map.of("name", "停用", "phone", "13800138000"), "key-disabled"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未发布");

        String rotated = formService.regenerateSecret(formId);
        assertThat(rotated).isNotEqualTo(before);
        assertThat(formService.detail(formId).getServerSecret()).isEqualTo(rotated);
    }

    private Long createPublishedForm(String formCode) {
        SaveInformationFormCommand command = new SaveInformationFormCommand();
        command.setFormCode(formCode);
        command.setFormName("客户联系表");
        command.setDescription("集成测试表单");
        command.setDefinitionJson(DEFINITION);
        command.setSuccessMessage("提交成功");
        command.setStatus(InformationFormStatus.PUBLISHED);
        return formService.save(command);
    }

    private SubmitInformationCommand submitCommand(String formCode, Map<String, Object> values, String idempotencyKey) {
        SubmitInformationCommand command = new SubmitInformationCommand();
        command.setFormCode(formCode);
        command.setValues(new java.util.LinkedHashMap<>(values));
        command.setSourcePage("integration-test");
        command.setIdempotencyKey(idempotencyKey);
        return command;
    }

    private boolean process(Long id, InformationSubmissionStatus status) {
        ProcessInformationSubmissionCommand command = new ProcessInformationSubmissionCommand();
        command.setId(id);
        command.setStatus(status);
        command.setProcessRemark("集成测试处理");
        return submissionService.process(command);
    }

    private void rebuildSchema() {
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
                  server_secret_digest varchar(128) NOT NULL,
                  created_by bigint DEFAULT NULL,
                  created_at datetime DEFAULT NULL,
                  updated_by bigint DEFAULT NULL,
                  updated_at datetime DEFAULT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_information_form_tenant_code (tenant_id, form_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE information_submission (
                  id bigint NOT NULL,
                  tenant_id varchar(64) NOT NULL,
                  org_id bigint DEFAULT NULL,
                  form_id bigint NOT NULL,
                  form_code varchar(64) NOT NULL,
                  values_json text NOT NULL,
                  source_page varchar(500) DEFAULT NULL,
                  idempotency_key varchar(128) DEFAULT NULL,
                  status varchar(32) NOT NULL DEFAULT 'PENDING',
                  process_remark varchar(1000) DEFAULT NULL,
                  submitted_at datetime DEFAULT NULL,
                  created_by bigint DEFAULT NULL,
                  created_at datetime DEFAULT NULL,
                  updated_by bigint DEFAULT NULL,
                  updated_at datetime DEFAULT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_information_submission_idempotency (tenant_id, form_id, idempotency_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE information_submission_snapshot (
                  id bigint NOT NULL,
                  tenant_id varchar(64) NOT NULL,
                  org_id bigint DEFAULT NULL,
                  submission_id bigint NOT NULL,
                  form_id bigint NOT NULL,
                  definition_json text NOT NULL,
                  created_by bigint DEFAULT NULL,
                  created_at datetime DEFAULT NULL,
                  updated_by bigint DEFAULT NULL,
                  updated_at datetime DEFAULT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_information_snapshot_submission (submission_id)
                )
                """);
    }

    private int intValue(String table, String expression) {
        Integer value = jdbcTemplate.queryForObject("SELECT " + expression + " FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private int intValue(String table, String expression, String where) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT " + expression + " FROM " + table + " WHERE " + where, Integer.class);
        return value == null ? 0 : value;
    }

    private Long longValue(String table, String column, String where) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE " + where, Long.class);
    }

    private String stringValue(String table, String column, String where) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE " + where, String.class);
    }

    @Configuration
    @ComponentScan(basePackages = "io.mango.information.core.service.impl")
    @org.mybatis.spring.annotation.MapperScan(basePackages = "io.mango.information.core.mapper")
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
