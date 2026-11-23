package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.workflow.core.model.WorkflowApplyAttributeCondition;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskAssigneeScope;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import io.mango.workflow.core.model.WorkflowTaskIdentityLinkRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.session.SqlSessionFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务历史只读查询集成测试。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        WorkflowTaskHistoryMapperIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow_task_history;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/workflow/*.xml"
})
class WorkflowTaskHistoryMapperIntegrationTest {

    private static final String BUSINESS_TYPE = "BAOHAN_RC_PROJECT_REVIEW";
    private static final String COMPLIANCE_BUSINESS_TYPE = "BAOHAN_COMPLIANCE_REVIEW";
    private static final String CONTRACT_BUSINESS_TYPE = "BAOHAN_CONTRACT_REVIEW";
    private static final Long TENANT_ID = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowTaskHistoryMapper taskHistoryMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void rebuild() {
        rebuildTables();
        insertData();
    }

    @Test
    void returnsAllTasksOfOneBusinessKeyInCreateTimeDescOrder() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);
        assertThat(rows).extracting(WorkflowTaskHistoryRow::getTaskId).containsExactly("T2", "T1");
        assertThat(rows.get(1).getClaimTime()).isNotNull();
        assertThat(rows.get(1).getEndTime()).isNotNull();
        assertThat(rows.get(0).getOrgId()).isEqualTo(200L);
        assertThat(rows.get(0).getApplyStatus()).isEqualTo("IN_APPROVAL");
    }

    @Test
    void readsAssignSourceAndSnapshotClaimTimeWithoutDuplicatingRows() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);

        WorkflowTaskHistoryRow dispatched = rows.get(1);
        assertThat(dispatched.getTaskId()).isEqualTo("T1");
        assertThat(dispatched.getAssignSource()).isEqualTo("AUTO_ASSIGN");
        assertThat(dispatched.getSnapshotClaimTime()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0, 45));
        WorkflowTaskHistoryRow unassigned = rows.get(0);
        assertThat(unassigned.getTaskId()).isEqualTo("T2");
        assertThat(unassigned.getAssignSource()).isNull();
        assertThat(unassigned.getSnapshotClaimTime()).isNull();
    }

    @Test
    void returnsAllTasksOfBusinessTypeWhenBusinessKeysAbsent() {
        WorkflowTaskHistoryCriteria absent = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria empty = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of())
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.countTasks(absent)).isEqualTo(3L);
        assertThat(taskHistoryMapper.selectTasks(absent, 2, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3", "T2");
        assertThat(taskHistoryMapper.countTasks(empty)).isEqualTo(3L);
    }

    @Test
    void filtersByBusinessKeysBatch() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC1", "RC2"));

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(3L);
        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactlyInAnyOrder("T1", "T2", "T3");
    }

    @Test
    void keepsSingleBusinessTypeEqualityWhenBusinessTypesAbsent() {
        WorkflowTaskHistoryCriteria single = criteria(List.of("RC1"));
        WorkflowTaskHistoryCriteria emptyList = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessTypes(List.of())
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        // 不传新字段时生成的 SQL 仍是单值等值，且空集合不会退化成 in ()。
        String sql = boundSql("countTasks", single);
        assertThat(sql).contains("a.business_type = ?");
        assertThat(sql).doesNotContain("a.business_type in");
        assertThat(boundSql("countTasks", emptyList)).isEqualTo(sql);
        assertThat(taskHistoryMapper.countTasks(emptyList)).isEqualTo(2L);
    }

    @Test
    void filtersByBusinessTypesList() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessTypes(List.of(BUSINESS_TYPE, COMPLIANCE_BUSINESS_TYPE))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(boundSql("countTasks", criteria)).contains("a.business_type in (", ")");
        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(4L);
        assertThat(taskIdsOf(criteria, 2, 0L)).containsExactly("T5", "T3");
        assertThat(taskIdsOf(criteria, 10, 0L)).containsExactly("T5", "T3", "T2", "T1");
    }

    @Test
    void filtersByProcessDefinitionKeys() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessTypes(List.of(BUSINESS_TYPE, COMPLIANCE_BUSINESS_TYPE, CONTRACT_BUSINESS_TYPE))
                .processDefinitionKeys(List.of("compliance_review"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(boundSql("countTasks", criteria)).contains("a.process_definition_key in (");
        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(1L);
        assertThat(taskIdsOf(criteria, 10, 0L)).containsExactly("T5");
    }

    @Test
    void intersectsBusinessTypesWithProcessDefinitionKeys() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessTypes(List.of(COMPLIANCE_BUSINESS_TYPE, CONTRACT_BUSINESS_TYPE))
                .processDefinitionKeys(List.of("compliance_review", "rc_project_review"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(1L);
        assertThat(taskIdsOf(criteria, 10, 0L)).containsExactly("T5");
    }

    @Test
    void appliesAttributeFiltersAcrossBusinessTypes() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessTypes(List.of(BUSINESS_TYPE, COMPLIANCE_BUSINESS_TYPE))
                .applyAttributeConditions(List.of(WorkflowApplyAttributeCondition.builder()
                        .key("risk_level")
                        .values(List.of("HIGH"))
                        .build()))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        // 属性子查询必须按同一业务类型口径收窄，否则多业务类型下 conditioned 过滤会漏命中。
        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(1L);
        assertThat(taskIdsOf(criteria, 10, 0L)).containsExactly("T5");
    }

    @Test
    void filtersByApplyCodeTitleAndSummaryKeyword() {
        assertThat(taskIdsOf(criteriaWithApplyKeyword("%GR20260102000002%"))).containsExactly("T3");
        assertThat(taskIdsOf(criteriaWithApplyKeyword("%中铁%"))).containsExactly("T3", "T2", "T1");
        assertThat(taskIdsOf(criteriaWithApplyKeyword("%履约保函%"))).containsExactly("T2", "T1");
    }

    @Test
    void treatsEscapedWildcardsInApplyKeywordAsLiterals() {
        WorkflowTaskHistoryCriteria criteria = criteriaWithApplyKeyword("%审核\\_中铁%");

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(2L);
        assertThat(taskIdsOf(criteria)).containsExactly("T2", "T1");
    }

    @Test
    void keepsApplyKeywordPageTotalConsistentWithRows() {
        WorkflowTaskHistoryCriteria criteria = criteriaWithApplyKeyword("%保函风控审核%");

        assertThat(taskHistoryMapper.countTasks(criteria)).isEqualTo(3L);
        assertThat(taskIdsOf(criteria, 2, 0L)).containsExactly("T3", "T2");
        assertThat(taskIdsOf(criteria, 2, 2L)).containsExactly("T1");
        assertThat(taskIdsOf(criteria, 3, 0L)).containsExactly("T3", "T2", "T1");
    }

    @Test
    void isolatesTenant() {
        WorkflowTaskHistoryCriteria otherTenant = WorkflowTaskHistoryCriteria.builder()
                .tenantId(2L)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC3"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.countTasks(otherTenant)).isEqualTo(1L);
        assertThat(taskHistoryMapper.selectTasks(criteria(List.of("RC3")), 10, 0L)).isEmpty();
    }

    @Test
    void filtersSelfScopeByAssigneeOrCandidate() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .selfScope(true)
                .selfIdentifiers(List.of("alice"))
                .selfCandidateGroups(List.of("ROLE_REVIEWER"))
                .sortColumn("createTime")
                .sortDirection("asc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1", "T2");
    }

    @Test
    void filtersSelfScopeByCandidateGroup() {
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .selfScope(true)
                .selfIdentifiers(List.of("nobody"))
                .selfCandidateGroups(List.of("ROLE_REVIEWER"))
                .sortColumn("createTime")
                .sortDirection("asc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(criteria, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
    }

    @Test
    void filtersTaskStatuses() {
        WorkflowTaskHistoryCriteria todoOnly = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(false)
                .includeCanceled(false)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria canceledOnly = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(false)
                .includeDone(false)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria doneOnly = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(false)
                .includeDone(true)
                .includeCanceled(false)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(todoOnly, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
        assertThat(taskIdsOf(canceledOnly, 10, 0L)).isEmpty();
        // T3 带 DELETE_REASON_，但它自己有 RETURN 处理记录：按任务级记录判为已办，不再混进「已终止」。
        assertThat(taskIdsOf(doneOnly, 10, 0L)).containsExactly("T3", "T1");
    }

    @Test
    void keepsGeneratedSqlStableWhenNewFiltersAreAbsent() {
        WorkflowTaskHistoryCriteria baseline = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria emptyLists = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .deptIds(List.of())
                .queryStatuses(List.of())
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(boundSql("countTasks", emptyLists)).isEqualTo(boundSql("countTasks", baseline));
        assertThat(boundSql("selectTasks", emptyLists)).isEqualTo(boundSql("selectTasks", baseline));
    }

    @Test
    void filtersByApplicantDeptIds() {
        assertThat(taskIdsOf(withDeptIds(200L))).containsExactly("T2", "T1");
        assertThat(taskIdsOf(withDeptIds(300L))).containsExactly("T3");
        assertThat(boundSql("countTasks", withDeptIds(200L))).contains("coalesce(a.applicant_dept_id, a.org_id) in (");
    }

    @Test
    void splitsDoneAndCanceledIntoDisjointDetailStatuses() {
        insertWithdrawnTask("T6", "P1", "bob");
        insertRejectedTask("T7", "P2", "bob");

        assertThat(taskIdsOf(withQueryStatuses("TODO"))).containsExactlyInAnyOrder("T2", "T5");
        assertThat(taskIdsOf(withQueryStatuses("COMPLETED"))).containsExactly("T1");
        assertThat(taskIdsOf(withQueryStatuses("RETURNED"))).containsExactly("T3");
        assertThat(taskIdsOf(withQueryStatuses("REJECTED"))).containsExactly("T7");
        assertThat(taskIdsOf(withQueryStatuses("WITHDRAWN"))).containsExactly("T6");
        assertThat(taskIdsOf(withQueryStatuses("DONE")))
                .containsExactlyInAnyOrder("T1", "T3", "T7");
        assertThat(taskIdsOf(withQueryStatuses("CANCELED"))).containsExactly("T6");
    }

    @Test
    void keepsDetailStatusesDisjointAndCoveringEveryRow() {
        insertWithdrawnTask("T6", "P1", "bob");
        insertRejectedTask("T7", "P2", "bob");
        insertTerminatedTask("T8", "carol");

        String[] detailStatuses = {"TODO", "COMPLETED", "REJECTED", "RETURNED", "WITHDRAWN", "TERMINATED"};
        List<Set<String>> rows = new ArrayList<>();
        long covered = 0L;
        for (String detailStatus : detailStatuses) {
            WorkflowTaskHistoryCriteria criteria = withQueryStatuses(detailStatus);
            long counted = taskHistoryMapper.countTasks(criteria);
            Set<String> taskIds = Set.copyOf(taskIdsOf(criteria));
            assertThat(taskIds).as("细状态 %s 的 total 与行数必须一致", detailStatus)
                    .hasSize((int) counted);
            rows.add(taskIds);
            covered += counted;
        }

        assertThat(covered).isEqualTo(taskHistoryMapper.countTasks(scopedCriteria().build()));
        for (int i = 0; i < rows.size(); i++) {
            for (int j = i + 1; j < rows.size(); j++) {
                assertThat(rows.get(i)).as("细状态 %s 与 %s 的行集不能相交",
                        detailStatuses[i], detailStatuses[j]).doesNotContainAnyElementsOf(rows.get(j));
            }
        }
    }

    @Test
    void treatsCollateralRejectionAsTerminated() {
        insertTerminatedTask("T8", "carol");

        assertThat(taskIdsOf(withQueryStatuses("TERMINATED"))).contains("T8");
        assertThat(taskIdsOf(withQueryStatuses("REJECTED"))).doesNotContain("T8");
        assertThat(taskIdsOf(withQueryStatuses("CANCELED"))).contains("T8");
    }

    @Test
    void coversEveryRowWhenDoneAndCanceledAreRequestedTogether() {
        insertWithdrawnTask("T6", "P1", "bob");
        insertRejectedTask("T7", "P2", "bob");

        List<String> todo = taskIdsOf(withQueryStatuses("TODO"));
        List<String> ended = taskIdsOf(scopedCriteria().build()).stream()
                .filter(taskId -> !todo.contains(taskId))
                .toList();
        List<String> doneAndCanceled = taskIdsOf(withQueryStatuses("DONE", "CANCELED"));

        assertThat(doneAndCanceled).containsExactlyInAnyOrderElementsOf(ended);
        assertThat(doneAndCanceled).doesNotContainAnyElementsOf(todo);
        assertThat(taskIdsOf(withQueryStatuses("DONE"))).doesNotContainAnyElementsOf(
                taskIdsOf(withQueryStatuses("CANCELED")));
    }

    @Test
    void keepsWithdrawnStatusFreeOfRejectedAndReturnedTasks() {
        jdbcTemplate.update("update workflow_business_apply set apply_status = ? where id = ?", "WITHDRAWN", 20L);
        insertWithdrawnTask("T6", "P2", "carol");

        // T3 有任务级 RETURN 记录，即使申请被撤回也不进 WITHDRAWN。
        List<String> withdrawn = taskIdsOf(withQueryStatuses("WITHDRAWN"));

        assertThat(withdrawn).containsExactly("T6");
        assertThat(taskIdsOf(withQueryStatuses("RETURNED"))).contains("T3");
    }

    @Test
    void narrowsAssigneeByIdScopeOrganizationRule() {
        WorkflowTaskAssigneeScope orgScope = new WorkflowTaskAssigneeScope(false, List.of("7001"),
                List.of(), List.of(), List.of());
        WorkflowTaskAssigneeScope selfScope = new WorkflowTaskAssigneeScope(false, List.of(),
                List.of("alice", "1001"), List.of(), List.of());
        WorkflowTaskAssigneeScope candidateScope = new WorkflowTaskAssigneeScope(false, List.of(),
                List.of(), List.of("alice"), List.of("ROLE_REVIEWER"));

        assertThat(taskIdsOf(scopedCriteria().assigneeScope(orgScope).build())).containsExactly("T1");
        assertThat(taskIdsOf(scopedCriteria().assigneeScope(selfScope).build())).containsExactly("T1");
        // 未领取任务只在「我是候选」时命中，命中条件是候选人与候选组两条之一。
        assertThat(taskIdsOf(scopedCriteria().assigneeScope(candidateScope).build())).containsExactly("T2");
        assertThat(taskIdsOf(scopedCriteria().assigneeScope(WorkflowTaskAssigneeScope.DENY_ALL).build())).isEmpty();
    }

    @Test
    void matchesAssigneeByIdScopeOrganizationOfOtherDepartment() {
        WorkflowTaskAssigneeScope otherDept = new WorkflowTaskAssigneeScope(false, List.of("7002"),
                List.of(), List.of(), List.of());

        assertThat(taskIdsOf(scopedCriteria().assigneeScope(otherDept).build())).containsExactly("T3");
    }

    @Test
    void keepsTaskCanceledAfterWithdrawalEvenWhenSiblingReturned() {
        jdbcTemplate.update("update workflow_business_apply set apply_status = ? where id = ?", "WITHDRAWN", 20L);
        insertTask("T6", "初审", "FIRST_REVIEW", "P2", "bob",
                LocalDateTime.of(2026, 1, 6, 10, 0), null, null, "申请人撤回");
        WorkflowTaskHistoryCriteria canceledOnly = scopedCriteria()
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(false)
                .includeDone(false)
                .includeCanceled(true)
                .build();
        WorkflowTaskHistoryCriteria doneOnly = scopedCriteria()
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(false)
                .includeDone(true)
                .includeCanceled(false)
                .build();

        // T3 在同一实例里有 RETURN 记录，后来被撕回：它已经是已办；T6 没有处理记录，仍是已终止。
        assertThat(taskIdsOf(doneOnly, 10, 0L)).containsExactly("T3", "T1");
        assertThat(taskIdsOf(canceledOnly, 10, 0L)).containsExactly("T6");
    }

    @Test
    void exposesRejectionRecordFlagPerTask() {
        insertTask("T6", "初审", "FIRST_REVIEW", "P2", "bob",
                LocalDateTime.of(2026, 1, 6, 10, 0), null, null, "审批驳回，流程实例已删除");
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                102L, TENANT_ID, "P2", "T3", "FIRST_REVIEW", "REJECT", null,
                LocalDateTime.of(2026, 1, 3, 11, 0));

        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria(List.of("RC2")), 10, 0L);

        assertThat(rows).extracting(WorkflowTaskHistoryRow::getTaskId).containsExactly("T6", "T3");
        assertThat(rows.get(0).getRejectedByRecord()).isFalse();
        assertThat(rows.get(1).getRejectedByRecord()).isTrue();
    }

    @Test
    void filtersByClaimStatusAndAssignee() {
        WorkflowTaskHistoryCriteria unclaimed = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .assigneeRequired(false)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria byAssignee = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .assigneeIds(List.of(1001L))
                .assigneeIdentifiers(List.of("1001"))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(unclaimed, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T2");
        assertThat(taskHistoryMapper.selectTasks(byAssignee, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1");
    }

    @Test
    void sortsEndTimeWithNullsLastInBothDirections() {
        WorkflowTaskHistoryCriteria asc = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("endTime")
                .sortDirection("asc")
                .build();
        WorkflowTaskHistoryCriteria desc = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("endTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(asc, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T1", "T3", "T2");
        assertThat(taskHistoryMapper.selectTasks(desc, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3", "T1", "T2");
    }

    @Test
    void filtersByApplyStatusAndReturnedFlag() {
        WorkflowTaskHistoryCriteria rejected = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyStatuses(List.of("REJECTED"))
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
        WorkflowTaskHistoryCriteria returned = WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(List.of("RC1", "RC2"))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .returned(Boolean.TRUE)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();

        assertThat(taskHistoryMapper.selectTasks(rejected, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactly("T3");
        assertThat(taskHistoryMapper.selectTasks(returned, 10, 0L))
                .extracting(WorkflowTaskHistoryRow::getTaskId)
                .containsExactlyInAnyOrder("T1", "T2", "T3");
    }

    @Test
    void exposesTaskLevelReturnFactAndLatestReturnTarget() {
        WorkflowTaskHistoryCriteria criteria = criteria(List.of("RC2"));

        List<WorkflowTaskHistoryRow> rows = taskHistoryMapper.selectTasks(criteria, 10, 0L);

        assertThat(rows).hasSize(1);
        WorkflowTaskHistoryRow canceled = rows.get(0);
        assertThat(canceled.getTaskId()).isEqualTo("T3");
        assertThat(canceled.getDeleteReason()).isEqualTo("审批驳回");
        assertThat(canceled.getReturned()).isTrue();
        assertThat(canceled.getTaskReturned()).isTrue();
        assertThat(canceled.getLastReturnAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 12, 0));
        assertThat(canceled.getLastReturnVariables()).contains("risk_specialist_review");
    }

    @Test
    void returnsCandidateLinksForRequestedTasks() {
        List<WorkflowTaskIdentityLinkRow> links = taskHistoryMapper.selectCandidateLinks(List.of("T2"));

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getUserId()).isEqualTo("alice");
        assertThat(links.get(0).getGroupId()).isEqualTo("ROLE_REVIEWER");
    }

    private WorkflowTaskHistoryCriteria criteria(List<String> businessKeys) {
        return WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .businessKeys(businessKeys)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
    }

    private WorkflowTaskHistoryCriteria criteriaWithApplyKeyword(String applyKeyword) {
        return WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessType(BUSINESS_TYPE)
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .applyKeyword(applyKeyword)
                .sortColumn("createTime")
                .sortDirection("desc")
                .build();
    }

    private List<String> taskIdsOf(WorkflowTaskHistoryCriteria criteria) {
        return taskIdsOf(criteria, 10, 0L);
    }

    private WorkflowTaskHistoryCriteria withDeptIds(Long... deptIds) {
        return scopedCriteria().deptIds(List.of(deptIds)).build();
    }

    private WorkflowTaskHistoryCriteria withQueryStatuses(String... statuses) {
        return scopedCriteria().queryStatuses(List.of(statuses)).build();
    }

    /**
     * 撤回场景：申请状态必须是 {@code WITHDRAWN}，否则任务只会落到「其他终止」。
     */
    private void insertWithdrawnTask(String taskId, String processInstanceId, String assignee) {
        if ("P1".equals(processInstanceId)) {
            insertApply(50L, TENANT_ID, "RC5", "WITHDRAWN", 600L, "P5",
                    "GR20260105000005", "保函风控审核-客户撤回", "天津港 / 履约保函");
            processInstanceId = "P5";
        }
        insertTask(taskId, "初审", "FIRST_REVIEW", processInstanceId, assignee,
                LocalDateTime.of(2026, 1, 6, 10, 0), null, null, "申请人撤回");
    }

    /**
     * 被他人驳回牵连作废：任务被删除、本人没有驳回/退回记录、申请状态是 {@code REJECTED}，
     * 按兜底桶落到 {@code TERMINATED}。
     */
    private void insertTerminatedTask(String taskId, String assignee) {
        insertApply(51L, TENANT_ID, "RC6", "REJECTED", 600L, "P6",
                "GR20260106000006", "保函风控审核-被牵连作废", "天津港 / 履约保函");
        insertTask(taskId, "初审", "FIRST_REVIEW", "P6", assignee,
                LocalDateTime.of(2026, 1, 8, 10, 0), null, null, "驳回终止");
    }

    private void insertRejectedTask(String taskId, String processInstanceId, String assignee) {
        insertTask(taskId, "初审", "FIRST_REVIEW", processInstanceId, assignee,
                LocalDateTime.of(2026, 1, 7, 10, 0), null, null, "审批驳回");
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                201L, TENANT_ID, processInstanceId, taskId, "FIRST_REVIEW", "REJECT", null,
                LocalDateTime.of(2026, 1, 7, 10, 5));
    }

    private WorkflowTaskHistoryCriteria.WorkflowTaskHistoryCriteriaBuilder scopedCriteria() {
        return WorkflowTaskHistoryCriteria.builder()
                .tenantId(TENANT_ID)
                .businessTypes(List.of(BUSINESS_TYPE, COMPLIANCE_BUSINESS_TYPE))
                .includeTodo(true)
                .includeDone(true)
                .includeCanceled(true)
                .sortColumn("createTime")
                .sortDirection("desc");
    }

    private String boundSql(String statement, WorkflowTaskHistoryCriteria criteria) {
        return sqlSessionFactory.getConfiguration()
                .getMappedStatement(WorkflowTaskHistoryMapper.class.getName() + "." + statement)
                .getBoundSql(Map.of("c", criteria))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> taskIdsOf(WorkflowTaskHistoryCriteria criteria, int limit, long offset) {
        return taskHistoryMapper.selectTasks(criteria, limit, offset).stream()
                .map(WorkflowTaskHistoryRow::getTaskId)
                .toList();
    }

    private void insertData() {
        insertApply(10L, TENANT_ID, "RC1", "IN_APPROVAL", 200L, "P1",
                "GR20260101000001", "保函风控审核_中铁三局", "廊坊硕坚 / 履约保函");
        insertApply(20L, TENANT_ID, "RC2", "REJECTED", 300L, "P2",
                "GR20260102000002", "保函风控审核X中铁四局", "福建金鼎 / 预付款保函");
        insertApply(30L, 2L, "RC3", "IN_APPROVAL", 400L, "P3",
                "GR20260103000003", "保函风控审核-荣昌区管网", "安徽金旗 / 履约保函");
        insertApply(40L, TENANT_ID, "RC4", COMPLIANCE_BUSINESS_TYPE, "compliance_review", "IN_APPROVAL", 500L, "P4",
                "GR20260104000004", "合规复核-中铁一局", "合规 / 反洗钱");
        insertTask("T1", "初审", "FIRST_REVIEW", "P1", "alice",
                LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 10, 0, 30),
                LocalDateTime.of(2026, 1, 1, 11, 0), null);
        insertTask("T2", "复审", "RECHECK", "P1", null,
                LocalDateTime.of(2026, 1, 2, 10, 0), null, null, null);
        insertTask("T3", "初审", "FIRST_REVIEW", "P2", "bob",
                LocalDateTime.of(2026, 1, 3, 10, 0), null,
                LocalDateTime.of(2026, 1, 3, 11, 0), "审批驳回");
        insertTask("T4", "初审", "FIRST_REVIEW", "P3", "carol",
                LocalDateTime.of(2026, 1, 4, 10, 0), null, null, null);
        insertTask("T5", "初审", "FIRST_REVIEW", "P4", "dave",
                LocalDateTime.of(2026, 1, 5, 10, 0), null, null, null);
        jdbcTemplate.update("insert into workflow_business_apply_attribute "
                        + "(id, tenant_id, business_type, apply_id, attr_key, attr_value) values (?, ?, ?, ?, ?, ?)",
                1L, TENANT_ID, COMPLIANCE_BUSINESS_TYPE, 40L, "risk_level", "HIGH");
        jdbcTemplate.update("insert into workflow_business_apply_attribute "
                        + "(id, tenant_id, business_type, apply_id, attr_key, attr_value) values (?, ?, ?, ?, ?, ?)",
                2L, TENANT_ID, BUSINESS_TYPE, 10L, "risk_level", "LOW");
        jdbcTemplate.update("insert into act_hi_identitylink (ID_, TASK_ID_, TYPE_, USER_ID_, GROUP_ID_) "
                + "values (?, ?, ?, ?, ?)", "IL1", "T2", "candidate", "alice", "ROLE_REVIEWER");
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                100L, TENANT_ID, "P1", "T1", "FIRST_REVIEW", "RETURN",
                "{\"targetTaskDefinitionKey\":\"FIRST_REVIEW\"}", LocalDateTime.of(2026, 1, 2, 9, 0));
        jdbcTemplate.update("insert into workflow_task_record "
                        + "(id, tenant_id, process_instance_id, task_id, task_definition_key, action, "
                        + "variables_json, created_time) values (?, ?, ?, ?, ?, ?, ?, ?)",
                101L, TENANT_ID, "P2", "T3", "FIRST_REVIEW", "RETURN",
                "{\"targetTaskDefinitionKey\":\"risk_specialist_review\"}", LocalDateTime.of(2026, 1, 3, 12, 0));
        jdbcTemplate.update("insert into workflow_business_apply_current_task "
                        + "(id, apply_id, process_instance_id, task_id, assign_source, claim_time, arrived_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                9001L, 10L, "P1", "T1", "AUTO_ASSIGN",
                LocalDateTime.of(2026, 1, 1, 10, 0, 45), LocalDateTime.of(2026, 1, 1, 10, 0));
        jdbcTemplate.update("insert into workflow_business_apply_current_task "
                        + "(id, apply_id, process_instance_id, task_id, assign_source, claim_time, arrived_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                9002L, 10L, "P1", "T2", null, null, LocalDateTime.of(2026, 1, 2, 10, 0));
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1001L, "alice");
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1002L, "bob");
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1003L, "carol");
        jdbcTemplate.update("insert into identity_user (id, username) values (?, ?)", 1004L, "dave");
        insertMember(5001L, 1001L, 7001L);
        insertMember(5002L, 1002L, 7002L);
        insertMember(5003L, 1003L, 7003L);
        insertMember(5004L, 1004L, 7003L);
        insertOrg(6001L, 0L);
        insertOrg(6002L, 0L);
        insertOrg(7001L, 6001L);
        insertOrg(7002L, 6001L);
        insertOrg(7004L, 6002L);
        insertOrg(7003L, 7004L);
    }

    private void insertMember(Long memberId, Long userId, Long orgId) {
        jdbcTemplate.update("insert into tenant_member (id, tenant_id, user_id, status, left_at) "
                + "values (?, ?, ?, ?, ?)", memberId, TENANT_ID, userId, 1, null);
        jdbcTemplate.update("insert into tenant_member_org (id, tenant_id, member_id, org_id) "
                + "values (?, ?, ?, ?)", memberId + 90_000L, TENANT_ID, memberId, orgId);
    }

    private void insertOrg(Long id, Long pid) {
        jdbcTemplate.update("insert into sys_org (id, tenant_id, pid, org_name, org_code, org_type) "
                + "values (?, ?, ?, ?, ?, ?)", id, String.valueOf(TENANT_ID), pid,
                "组织" + id, "ORG" + id, pid == 0L ? 2 : 3);
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String status,
                             Long deptId, String processInstanceId,
                             String applyCode, String applyTitle, String applySummary) {
        insertApply(id, tenantId, businessKey, BUSINESS_TYPE, "rc_project_review", status, deptId,
                processInstanceId, applyCode, applyTitle, applySummary);
    }

    private void insertApply(Long id, Long tenantId, String businessKey, String businessType,
                             String processDefinitionKey, String status,
                             Long deptId, String processInstanceId,
                             String applyCode, String applyTitle, String applySummary) {
        jdbcTemplate.update("insert into workflow_business_apply "
                        + "(id, tenant_id, org_id, business_type, business_key, apply_status, "
                        + "applicant_dept_id, process_instance_id, process_name, process_definition_key, "
                        + "apply_code, apply_title, apply_summary) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, deptId, businessType, businessKey, status, deptId, processInstanceId,
                "项目审批", processDefinitionKey, applyCode, applyTitle, applySummary);
    }

    private void insertTask(String taskId, String name, String taskDefinitionKey, String processInstanceId,
                            String assignee, LocalDateTime createTime, LocalDateTime claimTime,
                            LocalDateTime endTime, String deleteReason) {
        jdbcTemplate.update("insert into act_hi_taskinst "
                        + "(ID_, NAME_, TASK_DEF_KEY_, PROC_INST_ID_, PROC_DEF_ID_, ASSIGNEE_, "
                        + "START_TIME_, CLAIM_TIME_, END_TIME_, DELETE_REASON_) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                taskId, name, taskDefinitionKey, processInstanceId, "PD1", assignee,
                createTime, claimTime, endTime, deleteReason);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists act_hi_taskinst");
        jdbcTemplate.execute("drop table if exists act_hi_identitylink");
        jdbcTemplate.execute("drop table if exists act_ru_identitylink");
        jdbcTemplate.execute("drop table if exists workflow_business_apply");
        jdbcTemplate.execute("drop table if exists workflow_task_record");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_current_task");
        jdbcTemplate.execute("drop table if exists workflow_business_apply_attribute");
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("""
                create table act_hi_taskinst (
                    ID_ varchar(64) not null,
                    NAME_ varchar(255),
                    TASK_DEF_KEY_ varchar(128),
                    PROC_INST_ID_ varchar(64),
                    PROC_DEF_ID_ varchar(128),
                    ASSIGNEE_ varchar(128),
                    START_TIME_ timestamp,
                    CLAIM_TIME_ timestamp,
                    END_TIME_ timestamp,
                    DELETE_REASON_ varchar(255),
                    primary key (ID_)
                )
                """);
        jdbcTemplate.execute("""
                create table act_hi_identitylink (
                    ID_ varchar(64) not null,
                    TASK_ID_ varchar(64),
                    TYPE_ varchar(64),
                    USER_ID_ varchar(128),
                    GROUP_ID_ varchar(128),
                    primary key (ID_)
                )
                """);
        jdbcTemplate.execute("""
                create table act_ru_identitylink (
                    ID_ varchar(64) not null,
                    TASK_ID_ varchar(64),
                    TYPE_ varchar(64),
                    USER_ID_ varchar(128),
                    GROUP_ID_ varchar(128),
                    primary key (ID_)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply (
                    id bigint not null,
                    tenant_id bigint,
                    org_id bigint,
                    business_type varchar(128),
                    business_key varchar(128),
                    apply_status varchar(64),
                    applicant_dept_id bigint,
                    process_instance_id varchar(128),
                    process_name varchar(255),
                    process_definition_key varchar(128),
                    apply_code varchar(128),
                    apply_title varchar(255),
                    apply_summary varchar(1000),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_current_task (
                    id bigint not null,
                    apply_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    assign_source varchar(64),
                    claim_time timestamp,
                    arrived_at timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_task_record (
                    id bigint not null,
                    tenant_id bigint,
                    process_instance_id varchar(128),
                    task_id varchar(128),
                    task_definition_key varchar(128),
                    action varchar(64),
                    variables_json text,
                    created_time timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table workflow_business_apply_attribute (
                    id bigint not null,
                    tenant_id bigint,
                    business_type varchar(128),
                    apply_id bigint,
                    attr_key varchar(128),
                    attr_value varchar(255),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table identity_user (
                    id bigint not null,
                    username varchar(128),
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("drop table if exists tenant_member");
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint not null,
                    tenant_id bigint,
                    user_id bigint,
                    status tinyint default 1,
                    left_at datetime,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("drop table if exists tenant_member_org");
        jdbcTemplate.execute("""
                create table tenant_member_org (
                    id bigint not null,
                    tenant_id bigint,
                    member_id bigint,
                    org_id bigint,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("drop table if exists sys_org");
        jdbcTemplate.execute("""
                create table sys_org (
                    id bigint not null,
                    tenant_id varchar(64) not null,
                    pid bigint not null default 0,
                    org_name varchar(100),
                    org_code varchar(50),
                    org_type int,
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
