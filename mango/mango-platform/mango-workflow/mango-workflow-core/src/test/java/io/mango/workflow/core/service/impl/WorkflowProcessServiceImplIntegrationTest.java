package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import io.mango.workflow.core.entity.WorkflowTaskRecord;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import io.mango.workflow.core.service.WorkflowTaskAdvanceResult;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowProcessServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_process_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class WorkflowProcessServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorkflowProcessServiceImpl service;
    @Autowired
    private WorkflowDefinitionMapper definitionMapper;
    @Autowired
    private WorkflowFormInstanceMapper formInstanceMapper;
    @Autowired
    private WorkflowTaskRecordMapper taskRecordMapper;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private CapturingBusinessApplyService businessApplyService;
    @Autowired
    private CapturingTaskRuntimeService taskRuntimeService;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(9001L, "1", "admin", "default", "USER", "ORG", 100L, "admin"));
        reset(runtimeService, taskService);
        businessApplyService.clear();
        taskRuntimeService.clear();
        rebuildTables();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void startInfersCustomPageRenderModeAndPersistsStartSnapshotsThroughRealMappers() {
        insertDefinition(publishedDefinition(1001L));
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessInstanceId()).thenReturn("proc-1");
        when(instance.getBusinessKey()).thenReturn("BIZ-1");
        when(instance.getProcessDefinitionId()).thenReturn("contract_seal_approval:1:1001");
        when(runtimeService.startProcessInstanceById(any(), any(), any(Map.class))).thenReturn(instance);
        ProcessInstanceQuery processInstanceQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("proc-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(instance);
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("proc-1")).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.listPage(0, 1)).thenReturn(List.of());

        StartWorkflowProcessCommand command = new StartWorkflowProcessCommand();
        command.setDefinitionId(1001L);
        command.setBusinessType("contract_seal");
        command.setBusinessKey("BIZ-1");
        command.setSnapshotRef("snapshot-1");
        command.setVariables(Map.of("amount", 1200));

        var started = service.start(command).getData();

        assertThat(started.getProcessInstanceId()).isEqualTo("proc-1");
        assertThat(started.getApplyId()).isEqualTo(2001L);
        CreateWorkflowBusinessApplyCommand applyCommand = businessApplyService.lastCreateCommand();
        assertThat(applyCommand.getRenderMode()).isEqualTo(WorkflowApplyRenderMode.CUSTOM_PAGE);
        assertThat(applyCommand.getFormKey()).isEqualTo("form_contract_seal_approval");
        assertThat(applyCommand.getFormVersion()).isEqualTo(3);
        assertThat(applyCommand.getFormJsonSnapshot()).contains("\"mode\":\"CUSTOM\"");
        assertThat(applyCommand.getVariables()).containsEntry("businessType", "contract_seal")
                .containsEntry("businessKey", "BIZ-1")
                .containsEntry("applyId", "2001");
        assertThat(businessApplyService.lastStartedProcessInstanceId()).isEqualTo("proc-1");
        assertThat(taskRuntimeService.lastAdvancedProcessInstanceId()).isEqualTo("proc-1");

        WorkflowFormInstance formInstance = formInstanceMapper.selectOne(new QueryWrapper<WorkflowFormInstance>()
                .eq("process_instance_id", "proc-1")
                .last("limit 1"));
        assertThat(formInstance).isNotNull();
        assertThat(formInstance.getDefinitionId()).isEqualTo(1001L);
        assertThat(formInstance.getBusinessKey()).isEqualTo("BIZ-1");
        assertThat(formInstance.getVariablesJson()).contains("\"businessType\":\"contract_seal\"")
                .contains("\"applyId\":\"2001\"")
                .contains("\"mangoInitiator\":\"admin\"");

        WorkflowTaskRecord record = taskRecordMapper.selectOne(new QueryWrapper<WorkflowTaskRecord>()
                .eq("process_instance_id", "proc-1")
                .last("limit 1"));
        assertThat(record).isNotNull();
        assertThat(record.getAction()).isEqualTo("START");
        assertThat(record.getOperatorId()).isEqualTo(9001L);
        assertThat(record.getOperatorName()).isEqualTo("admin");
        assertThat(record.getVariablesJson()).contains("\"businessKey\":\"BIZ-1\"");
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_form_instance");
        jdbcTemplate.execute("drop table if exists workflow_definition");
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
                create table workflow_form_instance (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    business_key varchar(128),
                    definition_id bigint,
                    definition_key varchar(128),
                    definition_name varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    form_code varchar(128),
                    form_json text,
                    variables_json text,
                    status varchar(64),
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
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_name varchar(255),
                    task_definition_key varchar(128),
                    action varchar(64),
                    action_name varchar(64),
                    operator_id bigint,
                    operator_name varchar(128),
                    comment text,
                    variables_json text,
                    created_time timestamp,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp,
                    primary key (id)
                )
                """);
    }

    private WorkflowDefinition publishedDefinition(Long id) {
        LocalDateTime now = LocalDateTime.parse("2026-06-27T10:00:00");
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(id);
        definition.setTenantId(1L);
        definition.setCategoryId(10L);
        definition.setDomainCode("CONTRACT");
        definition.setOrgId(100L);
        definition.setAdminUsers("[\"admin\"]");
        definition.setDefinitionName("合同用印审批");
        definition.setDefinitionKey("contract_seal_approval");
        definition.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        definition.setProcessDefinitionId("contract_seal_approval:1:1001");
        definition.setProcessDefinitionVersion(1);
        definition.setPublishedVersionNo(3);
        definition.setFormCode("form_contract_seal_approval");
        definition.setFormJson("""
                {"mode":"CUSTOM","customConfig":{"approvePageKey":"workflow.contractSeal.approve"}}
                """);
        definition.setRemark("合同用印");
        definition.setCreatedBy(9001L);
        definition.setCreatedTime(now);
        definition.setCreatedAt(now);
        definition.setUpdatedBy(9001L);
        definition.setUpdatedTime(now);
        definition.setUpdatedAt(now);
        return definition;
    }

    private void insertDefinition(WorkflowDefinition definition) {
        assertThat(definitionMapper.insert(definition)).isEqualTo(1);
    }

    @Configuration
    @Import(WorkflowProcessServiceImpl.class)
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
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        TaskService taskService() {
            return mock(TaskService.class);
        }

        @Bean
        HistoryService historyService() {
            return mock(HistoryService.class);
        }

        @Bean
        WorkflowEventPublisher workflowEventPublisher(ObjectProvider<io.mango.infra.event.api.IDomainEventPublisher> provider) {
            return new WorkflowEventPublisher(provider);
        }

        @Bean
        CapturingBusinessApplyService workflowBusinessApplyService() {
            return new CapturingBusinessApplyService();
        }

        @Bean
        CapturingTaskRuntimeService workflowTaskRuntimeService() {
            return new CapturingTaskRuntimeService();
        }
    }

    static class CapturingBusinessApplyService implements IWorkflowBusinessApplyService {

        private CreateWorkflowBusinessApplyCommand lastCreateCommand;
        private String lastStartedProcessInstanceId;

        void clear() {
            lastCreateCommand = null;
            lastStartedProcessInstanceId = null;
        }

        CreateWorkflowBusinessApplyCommand lastCreateCommand() {
            return lastCreateCommand;
        }

        String lastStartedProcessInstanceId() {
            return lastStartedProcessInstanceId;
        }

        @Override
        public R<WorkflowBusinessApplyVO> create(CreateWorkflowBusinessApplyCommand command) {
            this.lastCreateCommand = command;
            WorkflowBusinessApplyVO apply = new WorkflowBusinessApplyVO();
            apply.setId(2001L);
            apply.setBusinessType(command.getBusinessType());
            apply.setBusinessKey(command.getBusinessKey());
            return R.ok(apply);
        }

        @Override
        public void markProcessStarted(Long applyId, Long processDefinitionId, String processDefinitionKey,
                                       String engineProcessDefinitionId, String processName, String processInstanceId) {
            this.lastStartedProcessInstanceId = processInstanceId;
        }

        @Override
        public R<PageResult<WorkflowBusinessApplyVO>> page(WorkflowBusinessApplyPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<WorkflowBusinessApplySummaryVO> mySummary() {
            return R.ok(new WorkflowBusinessApplySummaryVO());
        }

        @Override
        public R<WorkflowBusinessApplyVO> detail(Long applyId) {
            return R.ok(null);
        }

        @Override
        public R<PageResult<WorkflowBusinessApplyVO>> history(String businessType, String businessKey,
                                                              WorkflowBusinessApplyPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<WorkflowBusinessApplyProgressVO> latestProgress(String businessType, String businessKey) {
            return R.ok(null);
        }

        @Override
        public Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType,
                                                                           Collection<String> businessKeys) {
            return Map.of();
        }

        @Override
        public List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType,
                                                                  Collection<String> businessKeys) {
            return List.of();
        }

        @Override
        public R<WorkflowBusinessApplyVO> byProcessInstance(String processInstanceId) {
            return R.ok(null);
        }

        @Override
        public WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId) {
            return null;
        }

        @Override
        public void refreshCurrentTasks(String processInstanceId) {
        }

        @Override
        public WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId) {
            return null;
        }

        @Override
        public void markApproved(String processInstanceId) {
        }

        @Override
        public void markRejected(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        }

        @Override
        public void markTerminated(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        }
    }

    static class CapturingTaskRuntimeService implements IWorkflowTaskRuntimeService {

        private String lastAdvancedProcessInstanceId;

        void clear() {
            lastAdvancedProcessInstanceId = null;
        }

        String lastAdvancedProcessInstanceId() {
            return lastAdvancedProcessInstanceId;
        }

        @Override
        public WorkflowTaskAdvanceResult advanceRuntimeTasks(String processInstanceId) {
            this.lastAdvancedProcessInstanceId = processInstanceId;
            return new WorkflowTaskAdvanceResult(processInstanceId, false, null);
        }

        @Override
        public R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<PageResult<WorkflowTaskVO>> copied(WorkflowTaskPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<WorkflowTaskSummaryVO> summary() {
            return R.ok(new WorkflowTaskSummaryVO());
        }

        @Override
        public R<WorkflowMyTaskSummaryVO> myTaskSummary() {
            return R.ok(new WorkflowMyTaskSummaryVO());
        }

        @Override
        public R<WorkflowTaskDetailVO> detail(String taskId) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> complete(CompleteWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<WorkflowTaskCompleteResultVO> completeWithResult(CompleteWorkflowTaskCommand command) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> reject(RejectWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<WorkflowTaskCompleteResultVO> returnTask(ReturnWorkflowTaskCommand command) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> saveDraft(SaveWorkflowTaskDraftCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> transfer(TransferWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> addSign(AddSignWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> claim(ClaimWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> unclaim(ClaimWorkflowTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> readCopied(ReadWorkflowCopiedTaskCommand command) {
            return R.ok(true);
        }

        @Override
        public R<WorkflowProcessDetailVO> processDetail(String processInstanceId) {
            return R.ok(null);
        }
    }
}
