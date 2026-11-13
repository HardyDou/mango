package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.api.query.WorkflowTaskTraceQuery;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 按业务键读取审批流转记录的集成测试。
 *
 * <p>使用真实 MyBatis-Plus 租户插件与 H2 表结构，覆盖多流程实例、过滤、排序、租户隔离、
 * 变量不返回和超限失败；租户插件保持平台默认配置，不做关闭或排除表覆盖。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskTraceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_trace;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/workflow/*.xml"
})
class WorkflowTaskTraceIntegrationTest {

    private static final String BUSINESS_TYPE = "BAOHAN_RC_PROJECT_REVIEW";
    private static final String BUSINESS_KEY = "RC1";
    private static final String TENANT_ID = "1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowTaskHistoryMapper taskHistoryMapper;

    @Autowired
    private WorkflowBusinessApplyMapper businessApplyMapper;

    @Autowired
    private WorkflowTaskRecordMapper taskRecordMapper;

    @Autowired
    private WorkflowBusinessApplyStatusLogMapper statusLogMapper;

    private WorkflowTaskQueryProperties properties;
    private WorkflowTaskBusinessQueryService service;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.request(null, null, TENANT_ID, null, null));
        rebuildTables();
        insertData();
        properties = new WorkflowTaskQueryProperties();
        properties.setUnpagedMaxRows(2000);
        @SuppressWarnings("unchecked")
        ObjectProvider<WorkflowBusinessApplyAccessChecker> checkerProvider = mock(ObjectProvider.class);
        when(checkerProvider.getIfAvailable()).thenReturn(null);
        service = new WorkflowTaskBusinessQueryService(taskHistoryMapper, businessApplyMapper, taskRecordMapper,
                statusLogMapper, checkerProvider, mock(WorkflowTaskQueryAuthorizer.class),
                mock(WorkflowAssigneeIdentityService.class), mock(WorkflowCandidateGroupProvider.class),
                properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void returnsEveryProcessInstanceOfBusinessKeyInTimeOrder() {
        List<WorkflowTaskRecordVO> records = service.traceByBusinessKey(query());

        assertThat(records).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(100L, 101L, 102L, 105L, 103L);
        assertThat(records).extracting(WorkflowTaskRecordVO::getProcessInstanceId)
                .containsExactly("P1", "P1", "P1", "P1", "P1B");
        assertThat(records).extracting(WorkflowTaskRecordVO::getActionName)
                .containsExactly("发起", "认领", "通过", "认领", "发起");
    }

    @Test
    void filtersByOperatorIdAndOperatorName() {
        WorkflowTaskTraceQuery query = query();
        query.setOperatorIds(List.of(1002L));

        assertThat(service.traceByBusinessKey(query)).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(101L, 102L);

        query.setOperatorIds(List.of(1001L));

        assertThat(service.traceByBusinessKey(query)).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(100L, 105L, 103L);
    }

    @Test
    void filtersByApplicantDeptOfWholeProcessInstance() {
        WorkflowTaskTraceQuery query = query();
        query.setDeptIds(List.of(300L));

        assertThat(service.traceByBusinessKey(query)).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(103L);
    }

    @Test
    void filtersByActionsAndTimeRange() {
        WorkflowTaskTraceQuery query = query();
        query.setActions(List.of("start", "COMPLETE"));

        assertThat(service.traceByBusinessKey(query)).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(100L, 102L, 103L);

        query.setActions(List.of());
        query.setBeginTime(LocalDateTime.of(2026, 1, 1, 10, 2));

        assertThat(service.traceByBusinessKey(query)).extracting(WorkflowTaskRecordVO::getId)
                .containsExactly(102L, 105L, 103L);
    }

    @Test
    void mergesWithdrawFromStatusLogIntoTimeLine() {
        jdbcTemplate.update("insert into workflow_business_apply_status_log "
                        + "(id, tenant_id, apply_id, action, action_name, operator_id, operator_name, comment, "
                        + "process_instance_id, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                200L, 1L, 10L, "WITHDRAW", "撤回", 1001L, "alice", "客户取消申请", "P1",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 30)));

        List<WorkflowTaskRecordVO> records = service.traceByBusinessKey(query());

        assertThat(records).extracting(WorkflowTaskRecordVO::getAction)
                .contains("WITHDRAW");
        assertThat(records).filteredOn(record -> "WITHDRAW".equals(record.getAction()))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getActionName()).isEqualTo("撤回");
                    assertThat(record.getComment()).isEqualTo("客户取消申请");
                    assertThat(record.getVariables()).isNull();
                });
    }

    @Test
    void hidesRecordsOfOtherTenant() {
        WorkflowTaskTraceQuery query = query();
        query.setBusinessKey("RC2");

        assertThat(service.traceByBusinessKey(query)).isEmpty();
    }

    @Test
    void returnsRecordsWithoutVariables() {
        assertThat(service.traceByBusinessKey(query()))
                .allSatisfy(record -> assertThat(record.getVariables()).isNull());
    }

    @Test
    void failsInsteadOfTruncatingWhenRecordCountExceedsLimit() {
        properties.setUnpagedMaxRows(2);

        assertThatThrownBy(() -> service.traceByBusinessKey(query()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("单次返回上限");
    }

    private WorkflowTaskTraceQuery query() {
        WorkflowTaskTraceQuery query = new WorkflowTaskTraceQuery();
        query.setBusinessType(BUSINESS_TYPE);
        query.setBusinessKey(BUSINESS_KEY);
        return query;
    }

    private void insertData() {
        insertApply(10L, 1L, "RC1", "P1", 200L);
        insertApply(11L, 1L, "RC1", "P1B", 300L);
        insertApply(20L, 2L, "RC2", "P2", 200L);
        insertRecord(100L, 1L, "P1", "START", 1001L, "alice", null,
                LocalDateTime.of(2026, 1, 1, 10, 0));
        insertRecord(101L, 1L, "P1", "CLAIM", 1002L, "bob", "认领任务",
                LocalDateTime.of(2026, 1, 1, 10, 1));
        insertRecord(102L, 1L, "P1", "COMPLETE", 1002L, "bob", "同意",
                LocalDateTime.of(2026, 1, 1, 10, 2));
        insertRecord(103L, 1L, "P1B", "START", 1001L, "alice", null,
                LocalDateTime.of(2026, 1, 1, 11, 0));
        insertRecord(104L, 2L, "P2", "START", 2001L, "carol", null,
                LocalDateTime.of(2026, 1, 1, 12, 0));
        insertRecord(105L, 1L, "P1", "CLAIM", null, "alice", "认领任务",
                LocalDateTime.of(2026, 1, 1, 10, 3));
        jdbcTemplate.update("insert into identity_user (id, tenant_id, username) values (?, ?, ?)",
                1001L, 1L, "alice");
        jdbcTemplate.update("insert into tenant_member (id, tenant_id, user_id) values (?, ?, ?)",
                5001L, 1L, 1001L);
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String processInstanceId, Long deptId) {
        jdbcTemplate.update("insert into workflow_business_apply "
                        + "(id, tenant_id, business_type, business_key, apply_status, applicant_dept_id, "
                        + "process_instance_id) values (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, BUSINESS_TYPE, businessKey, "IN_APPROVAL", deptId, processInstanceId);
    }

    private void insertRecord(Long id, Long tenantId, String processInstanceId, String action,
                              Long operatorId, String operatorName, String comment, LocalDateTime createdTime) {
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, action, operator_id, operator_name, comment, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, processInstanceId, action, operatorId, operatorName, comment,
                "{\"amount\":100}", createdTime);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_status_log");
        jdbcTemplate.execute("""
                create table workflow_business_apply_status_log (
                    id bigint not null,
                    tenant_id bigint,
                    apply_id bigint,
                    action varchar(32) not null,
                    action_name varchar(64) not null,
                    operator_id bigint,
                    operator_name varchar(128),
                    comment varchar(1000),
                    task_id varchar(128),
                    task_definition_key varchar(128),
                    process_instance_id varchar(128),
                    created_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("drop table if exists tenant_member");
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    tenant_id bigint,
                    business_type varchar(128),
                    business_key varchar(128),
                    affinity_key varchar(128),
                    apply_status varchar(64),
                    applicant_dept_id bigint,
                    process_instance_id varchar(128),
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
                    comment varchar(1000),
                    variables_json text,
                    created_time timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table identity_user (
                    id bigint not null,
                    tenant_id bigint,
                    username varchar(128),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint not null,
                    tenant_id bigint,
                    user_id bigint,
                    primary key (id)
                )
                """);
    }

    @Configuration
    @Import(MybatisPlusAutoConfiguration.class)
    @MapperScan("io.mango.workflow.core.mapper")
    static class TestConfig {
    }
}
