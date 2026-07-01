package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowDefinitionVersion;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowDefinitionServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_definition_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class WorkflowDefinitionServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorkflowDefinitionServiceImpl service;
    @Autowired
    private WorkflowDefinitionMapper definitionMapper;
    @Autowired
    private WorkflowDefinitionVersionMapper versionMapper;
    @Autowired
    private WorkflowCategoryMapper categoryMapper;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private WorkflowDesignerBpmnConverter bpmnConverter;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        reset(repositoryService, bpmnConverter);
        rebuildTables();
        insertCategory(10L, 1L, "PAYMENT_BUILTIN", "支付内置流程");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void pageFiltersByDataScopeThroughRealMapper() {
        insertDefinition(definition(1001L, 1L, 10L, "PAYMENT", 100L, 7001L,
                "合同用印审批", "CONTRACT_SEAL", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-24T10:00:00")));
        insertDefinition(definition(1002L, 1L, 10L, "PAYMENT", 999L, 9001L,
                "合同付款审批", "CONTRACT_PAY", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-24T11:00:00")));
        insertDefinition(definition(1003L, 1L, 10L, "PAYMENT", 999L, 7002L,
                "合同归档审批", "CONTRACT_ARCHIVE", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-24T12:00:00")));
        insertDefinition(definition(1004L, 1L, 10L, "RESOURCE", 100L, 7001L,
                "合同资源审批", "CONTRACT_RESOURCE", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-24T13:00:00")));

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setKeyword("合同");
        query.setDomainCode("PAYMENT");

        PageResult<WorkflowDefinitionVO> page = service.page(query).getData();

        assertThat(page.getList())
                .extracting(WorkflowDefinitionVO::getId)
                .containsExactly(1002L, 1001L);
    }

    @Test
    void getRejectsRowsOutsideDataScopeThroughRealMapper() {
        insertDefinition(definition(1101L, 1L, 10L, "PAYMENT", 999L, 7001L,
                "无权限流程", "DENIED_FLOW", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-24T10:00:00")));

        assertThatThrownBy(() -> service.get(1101L))
                .hasMessageContaining("流程定义不存在");
    }

    @Test
    void publishedPageReturnsVisiblePublishedSnapshotThroughRealMappers() {
        insertDefinition(definition(1201L, 1L, 10L, "PAYMENT", 100L, 7001L,
                "退款审批-草稿", "PAYMENT_REFUND", "PUBLISHED", 2, "proc-draft",
                LocalDateTime.parse("2026-06-25T10:00:00")));
        insertDefinition(definition(1202L, 1L, 10L, "PAYMENT", 100L, 7001L,
                "内嵌审批-草稿", "PAYMENT_EMBEDDED", "PUBLISHED", 1, "proc-hidden",
                LocalDateTime.parse("2026-06-25T11:00:00")));
        insertVersion(version(2201L, 1L, 1201L, 2, 10L, "PAYMENT", 100L, true,
                "退款审批", "PAYMENT_REFUND", "published-icon", "deploy-published",
                "proc-published", 3, LocalDateTime.parse("2026-06-26T09:30:00")));
        insertVersion(version(2202L, 1L, 1202L, 1, 10L, "PAYMENT", 100L, false,
                "内嵌审批", "PAYMENT_EMBEDDED", "hidden-icon", "deploy-hidden",
                "proc-hidden", 1, LocalDateTime.parse("2026-06-26T10:00:00")));

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setPublishedOnly(true);
        query.setStartEntryVisible(true);

        PageResult<WorkflowDefinitionVO> page = service.page(query).getData();

        assertThat(page.getList()).hasSize(1);
        WorkflowDefinitionVO vo = page.getList().get(0);
        assertThat(vo.getId()).isEqualTo(1201L);
        assertThat(vo.getDefinitionName()).isEqualTo("退款审批");
        assertThat(vo.getIcon()).isEqualTo("published-icon");
        assertThat(vo.getDesignerJson()).isEqualTo("{\"name\":\"PAYMENT_REFUND-published\"}");
        assertThat(vo.getFormJson()).isEqualTo("{\"mode\":\"published\"}");
        assertThat(vo.getProcessDefinitionId()).isEqualTo("proc-published");
        assertThat(vo.getPublishedVersionNo()).isEqualTo(2);
        assertThat(vo.getStartEntryVisible()).isTrue();
        assertThat(vo.getHasUnpublishedChanges()).isFalse();
    }

    @Test
    void deployInternalPublishesDefinitionAndVersionWithoutMemberDataScope() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(8801L, "55", "workflow-initializer", "INTERNAL",
                        "INTERNAL_USER", "INTERNAL_ORG", 55L, "workflow-starter"));
        insertDefinition(definition(1301L, 55L, 10L, "PAYMENT", 999L, 7001L,
                "退款审批", "PAYMENT_REFUND_APPROVAL", "DRAFT", null, null,
                LocalDateTime.parse("2026-06-25T12:00:00")));
        stubDeployment("PAYMENT_REFUND_APPROVAL", "退款审批", "55", "deploy-internal", "proc-internal", 1);

        var result = service.deployInternal(1301L).getData();

        assertThat(result.getDeploymentId()).isEqualTo("deploy-internal");
        assertThat(result.getProcessDefinitionId()).isEqualTo("proc-internal");
        WorkflowDefinition definition = definitionMapper.selectById(1301L);
        assertThat(definition.getStatus()).isEqualTo(WorkflowDefinitionStatus.PUBLISHED.name());
        assertThat(definition.getPublishedVersionNo()).isEqualTo(1);
        assertThat(definition.getDeploymentId()).isEqualTo("deploy-internal");
        WorkflowDefinitionVersion version = versionMapper.selectOne(new QueryWrapper<WorkflowDefinitionVersion>()
                .eq("definition_id", 1301L)
                .last("limit 1"));
        assertThat(version.getPublishStatus()).isEqualTo("SUCCESS");
        assertThat(version.getBpmnXml()).isEqualTo("<definitions />");
        assertThat(version.getDeploymentId()).isEqualTo("deploy-internal");
        assertThat(version.getProcessDefinitionId()).isEqualTo("proc-internal");
    }

    @Test
    void ensurePublishedCreatesCategoryDefinitionAndVersionThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(9001L, "1", "admin", "default", "USER", "ORG", 100L, "internal-admin"));
        stubDeployment("PAYMENT_REFUND_APPROVAL", "退款审批", "1", "deploy-created", "proc-created", 1);

        var result = service.ensurePublished(ensureCommand()).getData();

        assertThat(result.getDeploymentId()).isEqualTo("deploy-created");
        assertThat(result.getProcessDefinitionId()).isEqualTo("proc-created");
        assertThat(categoryMapper.selectCount(new QueryWrapper<WorkflowCategory>()
                .eq("domain_code", "PAYMENT")
                .eq("category_code", "PAYMENT_AUTO"))).isEqualTo(1L);
        WorkflowDefinition definition = definitionMapper.selectOne(new QueryWrapper<WorkflowDefinition>()
                .eq("definition_key", "PAYMENT_REFUND_APPROVAL")
                .last("limit 1"));
        assertThat(definition).isNotNull();
        assertThat(definition.getStatus()).isEqualTo(WorkflowDefinitionStatus.PUBLISHED.name());
        assertThat(definition.getDeploymentId()).isEqualTo("deploy-created");
        assertThat(definition.getOrgId()).isEqualTo(100L);
        WorkflowDefinitionVersion version = versionMapper.selectOne(new QueryWrapper<WorkflowDefinitionVersion>()
                .eq("definition_id", definition.getId())
                .last("limit 1"));
        assertThat(version.getVersionNo()).isEqualTo(1);
        assertThat(version.getPublishStatus()).isEqualTo("SUCCESS");
        assertThat(version.getProcessDefinitionId()).isEqualTo("proc-created");
    }

    @Test
    void ensurePublishedUpdatesChangedDefinitionAndCreatesNewVersionThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(9001L, "1", "admin", "default", "USER", "ORG", 100L, "internal-admin"));
        stubDeployment("PAYMENT_REFUND_APPROVAL", "退款审批", "1", "deploy-created", "proc-created", 1);
        service.ensurePublished(ensureCommand());
        WorkflowDefinition created = definitionMapper.selectOne(new QueryWrapper<WorkflowDefinition>()
                .eq("definition_key", "PAYMENT_REFUND_APPROVAL")
                .last("limit 1"));

        reset(repositoryService, bpmnConverter);
        stubDeployableAndDeployment("proc-created", "PAYMENT_REFUND_APPROVAL", "退款审批-新版",
                "1", "deploy-updated", "proc-updated", 2);
        EnsureWorkflowDefinitionCommand changed = ensureCommand();
        changed.setDefinitionName("退款审批-新版");
        changed.setStartEntryVisible(false);
        changed.setFormJson("{\"mode\":\"UPDATED\"}");

        var result = service.ensurePublished(changed).getData();

        assertThat(result.getDeploymentId()).isEqualTo("deploy-updated");
        assertThat(result.getProcessDefinitionId()).isEqualTo("proc-updated");
        WorkflowDefinition updated = definitionMapper.selectById(created.getId());
        assertThat(updated.getDefinitionName()).isEqualTo("退款审批-新版");
        assertThat(updated.getStartEntryVisible()).isFalse();
        assertThat(updated.getFormJson()).isEqualTo("{\"mode\":\"UPDATED\"}");
        assertThat(updated.getPublishedVersionNo()).isEqualTo(2);
        assertThat(updated.getDeploymentId()).isEqualTo("deploy-updated");
        assertThat(versionMapper.selectCount(new QueryWrapper<WorkflowDefinitionVersion>()
                .eq("definition_id", created.getId()))).isEqualTo(2L);
        WorkflowDefinitionVersion latest = versionMapper.selectOne(new QueryWrapper<WorkflowDefinitionVersion>()
                .eq("definition_id", created.getId())
                .eq("version_no", 2)
                .last("limit 1"));
        assertThat(latest.getPublishStatus()).isEqualTo("SUCCESS");
        assertThat(latest.getDefinitionName()).isEqualTo("退款审批-新版");
        assertThat(latest.getProcessDefinitionId()).isEqualTo("proc-updated");
    }

    private void stubDeployment(String definitionKey, String definitionName, String tenantId,
                                String deploymentId, String processDefinitionId, int processVersion) {
        BpmnModel bpmnModel = new BpmnModel();
        when(bpmnConverter.toModel(anyString(), eq(definitionKey), eq(definitionName))).thenReturn(bpmnModel);
        when(bpmnConverter.toXml(anyString(), eq(definitionKey), eq(definitionName))).thenReturn("<definitions />");
        DeploymentBuilder builder = mock(DeploymentBuilder.class);
        Deployment deployment = mock(Deployment.class);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(repositoryService.createDeployment()).thenReturn(builder);
        when(builder.name(definitionName)).thenReturn(builder);
        when(builder.key(definitionKey)).thenReturn(builder);
        when(builder.tenantId(tenantId)).thenReturn(builder);
        when(builder.addBpmnModel(definitionKey + ".bpmn20.xml", bpmnModel)).thenReturn(builder);
        when(builder.deploy()).thenReturn(deployment);
        when(deployment.getId()).thenReturn(deploymentId);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.deploymentId(deploymentId)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(definitionKey)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);
        when(processDefinition.getId()).thenReturn(processDefinitionId);
        when(processDefinition.getVersion()).thenReturn(processVersion);
    }

    private void stubDeployableAndDeployment(String existingProcessDefinitionId, String definitionKey,
                                             String definitionName, String tenantId, String deploymentId,
                                             String processDefinitionId, int processVersion) {
        BpmnModel bpmnModel = new BpmnModel();
        when(bpmnConverter.toModel(anyString(), eq(definitionKey), eq(definitionName))).thenReturn(bpmnModel);
        when(bpmnConverter.toXml(anyString(), eq(definitionKey), eq(definitionName))).thenReturn("<definitions />");
        ProcessDefinitionQuery deployableQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition existingProcessDefinition = mock(ProcessDefinition.class);
        DeploymentBuilder builder = mock(DeploymentBuilder.class);
        Deployment deployment = mock(Deployment.class);
        ProcessDefinitionQuery deploymentQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition deployedProcessDefinition = mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(deployableQuery, deploymentQuery);
        when(deployableQuery.processDefinitionId(existingProcessDefinitionId)).thenReturn(deployableQuery);
        when(deployableQuery.singleResult()).thenReturn(existingProcessDefinition);
        when(repositoryService.createDeployment()).thenReturn(builder);
        when(builder.name(definitionName)).thenReturn(builder);
        when(builder.key(definitionKey)).thenReturn(builder);
        when(builder.tenantId(tenantId)).thenReturn(builder);
        when(builder.addBpmnModel(definitionKey + ".bpmn20.xml", bpmnModel)).thenReturn(builder);
        when(builder.deploy()).thenReturn(deployment);
        when(deployment.getId()).thenReturn(deploymentId);
        when(deploymentQuery.deploymentId(deploymentId)).thenReturn(deploymentQuery);
        when(deploymentQuery.processDefinitionKey(definitionKey)).thenReturn(deploymentQuery);
        when(deploymentQuery.latestVersion()).thenReturn(deploymentQuery);
        when(deploymentQuery.singleResult()).thenReturn(deployedProcessDefinition);
        when(deployedProcessDefinition.getId()).thenReturn(processDefinitionId);
        when(deployedProcessDefinition.getVersion()).thenReturn(processVersion);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists workflow_definition_version");
        jdbcTemplate.execute("drop table if exists workflow_definition");
        jdbcTemplate.execute("drop table if exists workflow_category");
        jdbcTemplate.execute("drop table if exists workflow_node_definition");
        jdbcTemplate.execute("""
                create table workflow_category (
                    id bigint not null,
                    tenant_id bigint,
                    category_name varchar(128),
                    category_code varchar(64),
                    domain_code varchar(64),
                    sort int,
                    status int,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_time timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_definition (
                    id bigint not null,
                    tenant_id bigint,
                    category_id bigint,
                    domain_code varchar(64),
                    org_id bigint,
                    admin_users text,
                    start_entry_visible boolean,
                    icon varchar(512),
                    definition_name varchar(128),
                    definition_key varchar(128),
                    deployment_id varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    published_version_no int,
                    source_template_id bigint,
                    source_template_code varchar(128),
                    source_template_version int,
                    designer_json text,
                    bpmn_xml text,
                    form_code varchar(128),
                    form_json text,
                    status varchar(64),
                    last_deploy_time timestamp,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_time timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_definition_version (
                    id bigint not null,
                    tenant_id bigint,
                    definition_id bigint,
                    version_no int,
                    category_id bigint,
                    domain_code varchar(64),
                    org_id bigint,
                    admin_users text,
                    start_entry_visible boolean,
                    icon varchar(512),
                    definition_name varchar(128),
                    definition_key varchar(128),
                    remark varchar(255),
                    form_code varchar(128),
                    designer_json text,
                    form_json text,
                    bpmn_xml text,
                    deployment_id varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    publish_status varchar(64),
                    publish_message varchar(255),
                    created_by bigint,
                    publish_time timestamp,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_time timestamp,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_node_definition (
                    id bigint not null,
                    status int,
                    primary key (id)
                )
                """);
    }

    private WorkflowDefinition definition(Long id, Long tenantId, Long categoryId, String domainCode,
                                          Long orgId, Long createdBy, String name, String key,
                                          String status, Integer publishedVersionNo,
                                          String processDefinitionId, LocalDateTime updatedTime) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(id);
        definition.setTenantId(tenantId);
        definition.setCategoryId(categoryId);
        definition.setDomainCode(domainCode);
        definition.setOrgId(orgId);
        definition.setAdminUsers("[\"admin\"]");
        definition.setStartEntryVisible(true);
        definition.setIcon("draft-icon");
        definition.setDefinitionName(name);
        definition.setDefinitionKey(key);
        definition.setDesignerJson("{\"name\":\"" + key + "-draft\"}");
        definition.setFormCode(key.toLowerCase());
        definition.setFormJson("{\"mode\":\"draft\"}");
        definition.setStatus(status);
        definition.setPublishedVersionNo(publishedVersionNo);
        definition.setProcessDefinitionId(processDefinitionId);
        definition.setCreatedBy(createdBy);
        definition.setCreatedTime(updatedTime.minusDays(1));
        definition.setCreatedAt(updatedTime.minusDays(1));
        definition.setUpdatedBy(createdBy);
        definition.setUpdatedTime(updatedTime);
        definition.setUpdatedAt(updatedTime);
        definition.setLastDeployTime(updatedTime);
        return definition;
    }

    private WorkflowDefinitionVersion version(Long id, Long tenantId, Long definitionId, Integer versionNo,
                                              Long categoryId, String domainCode, Long orgId,
                                              Boolean startEntryVisible, String name, String key,
                                              String icon, String deploymentId, String processDefinitionId,
                                              Integer processDefinitionVersion, LocalDateTime publishTime) {
        WorkflowDefinitionVersion version = new WorkflowDefinitionVersion();
        version.setId(id);
        version.setTenantId(tenantId);
        version.setDefinitionId(definitionId);
        version.setVersionNo(versionNo);
        version.setCategoryId(categoryId);
        version.setDomainCode(domainCode);
        version.setOrgId(orgId);
        version.setAdminUsers("[\"admin\"]");
        version.setStartEntryVisible(startEntryVisible);
        version.setIcon(icon);
        version.setDefinitionName(name);
        version.setDefinitionKey(key);
        version.setRemark("published");
        version.setFormCode(key.toLowerCase());
        version.setDesignerJson("{\"name\":\"" + key + "-published\"}");
        version.setFormJson("{\"mode\":\"published\"}");
        version.setBpmnXml("<definitions />");
        version.setDeploymentId(deploymentId);
        version.setProcessDefinitionId(processDefinitionId);
        version.setProcessDefinitionVersion(processDefinitionVersion);
        version.setPublishStatus("SUCCESS");
        version.setPublishMessage("发布成功");
        version.setCreatedBy(9001L);
        version.setPublishTime(publishTime);
        version.setCreatedTime(publishTime);
        version.setCreatedAt(publishTime);
        version.setUpdatedBy(9001L);
        version.setUpdatedTime(publishTime);
        version.setUpdatedAt(publishTime);
        return version;
    }

    private void insertDefinition(WorkflowDefinition definition) {
        assertThat(definitionMapper.insert(definition)).isEqualTo(1);
    }

    private void insertVersion(WorkflowDefinitionVersion version) {
        assertThat(versionMapper.insert(version)).isEqualTo(1);
    }

    private void insertCategory(Long id, Long tenantId, String categoryCode, String categoryName) {
        jdbcTemplate.update("""
                insert into workflow_category (
                    id, tenant_id, category_name, category_code, domain_code, sort, status,
                    created_by, created_time, created_at, updated_by, updated_time, updated_at
                ) values (?, ?, ?, ?, 'PAYMENT', 10, 1, 9001, ?, ?, 9001, ?, ?)
                """, id, tenantId, categoryName, categoryCode, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private EnsureWorkflowDefinitionCommand ensureCommand() {
        EnsureWorkflowDefinitionCommand command = new EnsureWorkflowDefinitionCommand();
        command.setDomainCode("PAYMENT");
        command.setCategoryCode("PAYMENT_AUTO");
        command.setCategoryName("支付自动流程");
        command.setCategorySort(20);
        command.setOrgId(100L);
        command.setAdminUsers(List.of("admin"));
        command.setStartEntryVisible(true);
        command.setDefinitionName("退款审批");
        command.setDefinitionKey("PAYMENT_REFUND_APPROVAL");
        command.setDesignerJson("{\"id\":\"startEvent\"}");
        command.setFormCode("payment_refund_approval");
        command.setFormJson("{\"mode\":\"CUSTOM\"}");
        return command;
    }

    @Configuration
    @Import(WorkflowDefinitionServiceImpl.class)
    @MapperScan("io.mango.workflow.core.mapper")
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RepositoryService repositoryService() {
            return mock(RepositoryService.class);
        }

        @Bean
        WorkflowDesignerBpmnConverter bpmnConverter() {
            return mock(WorkflowDesignerBpmnConverter.class);
        }

        @Bean
        DataScopeApplier dataScopeApplier() {
            return new TestDataScopeApplier();
        }

        @Bean
        DomainApi domainApi() {
            return new DomainApi() {
                @Override
                public R<PageResult<DomainVO>> page(DomainPageQuery query) {
                    return R.ok(PageResult.of(List.of(), 0, 1, 10));
                }

                @Override
                public R<List<DomainVO>> tree(DomainPageQuery query) {
                    return R.ok(List.of());
                }

                @Override
                public R<List<DomainVO>> enabledTree() {
                    return R.ok(List.of(enabledDomain()));
                }

                @Override
                public R<DomainVO> detail(Long id) {
                    return R.ok(enabledDomain());
                }

                @Override
                public R<DomainVO> detailByCode(String domainCode) {
                    return R.ok(enabledDomain());
                }

                @Override
                public R<Long> create(CreateDomainCommand command) {
                    return R.ok(1L);
                }

                @Override
                public R<Boolean> update(UpdateDomainCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> updateStatus(UpdateDomainStatusCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> delete(Long id) {
                    return R.ok(true);
                }
            };
        }

        private static DomainVO enabledDomain() {
            DomainVO domain = new DomainVO();
            domain.setDomainCode("PAYMENT");
            domain.setStatus(1);
            return domain;
        }
    }

    private static class TestDataScopeApplier implements DataScopeApplier {

        @Override
        public <T> void apply(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
            assertThat(resourceCode).isEqualTo("workflow:definition:list");
            assertThat(mapping.orgField()).isEqualTo("org_id");
            assertThat(mapping.selfField()).isEqualTo("created_by");
            wrapper.and(condition -> condition.in(mapping.orgField(), List.of(100L, 200L))
                    .or()
                    .eq(mapping.selfField(), 9001L));
        }
    }
}
