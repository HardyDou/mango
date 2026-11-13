package io.mango.workflow.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.enums.WorkflowTaskQueryScope;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.mango.workflow.api.query.WorkflowTaskTraceQuery;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.api.enums.WorkflowTaskCancelCause;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 按业务主键查询任务的参数与范围规则测试。
 */
class WorkflowTaskBusinessQueryServiceTest {

    private WorkflowTaskHistoryMapper taskHistoryMapper;
    private WorkflowBusinessApplyMapper businessApplyMapper;
    private WorkflowTaskRecordMapper taskRecordMapper;
    private WorkflowTaskQueryAuthorizer authorizer;
    private WorkflowAssigneeIdentityService assigneeIdentityService;
    private WorkflowCandidateGroupProvider candidateGroupProvider;
    private ObjectProvider<WorkflowBusinessApplyAccessChecker> checkerProvider;
    private WorkflowTaskQueryProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WorkflowTaskBusinessQueryService service;

    @BeforeAll
    static void initLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WorkflowBusinessApplyEntity.class);
        TableInfoHelper.initTableInfo(assistant, WorkflowTaskRecordEntity.class);
    }

    @BeforeEach
    void setUp() {
        taskHistoryMapper = mock(WorkflowTaskHistoryMapper.class);
        businessApplyMapper = mock(WorkflowBusinessApplyMapper.class);
        taskRecordMapper = mock(WorkflowTaskRecordMapper.class);
        authorizer = mock(WorkflowTaskQueryAuthorizer.class);
        assigneeIdentityService = mock(WorkflowAssigneeIdentityService.class);
        candidateGroupProvider = mock(WorkflowCandidateGroupProvider.class);
        properties = new WorkflowTaskQueryProperties();
        properties.setUnpagedMaxRows(2000);
        @SuppressWarnings("unchecked")
        ObjectProvider<WorkflowBusinessApplyAccessChecker> provider = mock(ObjectProvider.class);
        checkerProvider = provider;
        when(checkerProvider.getIfAvailable()).thenReturn(null);
        when(candidateGroupProvider.currentCandidateGroups()).thenReturn(List.of("ROLE_REVIEWER"));
        service = new WorkflowTaskBusinessQueryService(taskHistoryMapper, businessApplyMapper, taskRecordMapper,
                checkerProvider, authorizer, assigneeIdentityService, candidateGroupProvider, properties, objectMapper);
        MangoContextHolder.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "alice", "default", "USER", "ORG", 1001L, "token"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void rejectsOrgScope() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.ORG);

        assertThatThrownBy(() -> service.pageByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("指定组织范围暂不支持");
    }

    @Test
    void rejectsDeptIdsOutsideOrgScope() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setDeptIds(List.of(200L));

        assertThatThrownBy(() -> service.pageByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("deptIds");
    }

    @Test
    void rejectsPageWithoutSize() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setPage(1);

        assertThatThrownBy(() -> service.pageByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分页参数必须成对出现");
    }

    @Test
    void rejectsInvalidTimeRange() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setCreateTimeBegin(LocalDateTime.of(2026, 2, 1, 0, 0));
        query.setCreateTimeEnd(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> service.pageByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("时间区间非法");
    }

    @Test
    void rejectsAllScopeWithoutPermission() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.ALL);
        when(authorizer.canQueryAll()).thenReturn(false);

        assertThatThrownBy(() -> service.pageByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权查询全量任务");
        verify(taskHistoryMapper, never()).selectTasks(any(), anyInt(), anyLong());
    }

    @Test
    void queriesAllScopeWithoutSelfFilterWhenPermissionGranted() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.ALL);
        when(authorizer.canQueryAll()).thenReturn(true);
        when(taskHistoryMapper.countTasks(any())).thenReturn(1L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong())).thenReturn(List.of(row()));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).selectTasks(captor.capture(), eq(2000), eq(0L));
        assertThat(captor.getValue().isSelfScope()).isFalse();
        assertThat(captor.getValue().getSelfIdentifiers()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getBusinessKey()).isEqualTo("RC1");
        assertThat(result.getList().get(0).getStatus()).isEqualTo(WorkflowTaskRuntimeStatus.DONE.name());
        assertThat(result.getList().get(0).getClaimStatus()).isEqualTo(WorkflowTaskClaimStatus.ASSIGNED);
    }

    @Test
    void queriesByBusinessTypeWithoutBusinessKeys() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setBusinessKeys(null);
        WorkflowBusinessApplyAccessChecker checker = mock(WorkflowBusinessApplyAccessChecker.class);
        when(checker.hasProvider("BAOHAN_RC_PROJECT_REVIEW")).thenReturn(true);
        when(checkerProvider.getIfAvailable()).thenReturn(checker);
        when(taskHistoryMapper.countTasks(any())).thenReturn(3L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong())).thenReturn(List.of(row()));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).countTasks(captor.capture());
        assertThat(captor.getValue().getBusinessKeys()).isEmpty();
        assertThat(captor.getValue().getBusinessType()).isEqualTo("BAOHAN_RC_PROJECT_REVIEW");
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getList()).hasSize(1);
        verifyNoInteractions(businessApplyMapper);
    }

    @Test
    void filtersDeniedAppliesWhenBusinessKeysPresent() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        WorkflowBusinessApplyAccessChecker checker = mock(WorkflowBusinessApplyAccessChecker.class);
        when(checker.hasProvider("BAOHAN_RC_PROJECT_REVIEW")).thenReturn(true);
        when(checker.isAllowed(any())).thenReturn(false);
        when(checkerProvider.getIfAvailable()).thenReturn(checker);
        WorkflowBusinessApplyEntity denied = new WorkflowBusinessApplyEntity();
        denied.setId(10L);
        when(businessApplyMapper.selectList(any())).thenReturn(List.of(denied));
        when(taskHistoryMapper.countTasks(any())).thenReturn(0L);

        service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).countTasks(captor.capture());
        assertThat(captor.getValue().getDeniedApplyIds()).containsExactly(10L);
        verify(businessApplyMapper).selectList(any());
    }

    @Test
    void treatsEmptyBusinessKeysAsUnrestricted() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setBusinessKeys(List.of());
        when(taskHistoryMapper.countTasks(any())).thenReturn(0L);

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).countTasks(captor.capture());
        assertThat(captor.getValue().getBusinessKeys()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verifyNoInteractions(businessApplyMapper);
    }

    @Test
    void appliesSelfScopeWithUserAndCandidateGroups() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        when(taskHistoryMapper.countTasks(any())).thenReturn(0L);

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).countTasks(captor.capture());
        assertThat(captor.getValue().isSelfScope()).isTrue();
        assertThat(captor.getValue().getSelfIdentifiers()).containsExactly("alice", "1001");
        assertThat(captor.getValue().getSelfCandidateGroups()).containsExactly("ROLE_REVIEWER");
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(taskHistoryMapper, never()).selectTasks(any(), anyInt(), anyLong());
    }

    @Test
    void mapsStatusesAndClaimStatusesToSqlFlags() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        query.setStatuses(List.of(WorkflowTaskRuntimeStatus.TODO));
        query.setClaimStatuses(List.of(WorkflowTaskClaimStatus.UNCLAIMED));
        query.setApplyStatuses(List.of(WorkflowApplyStatus.WITHDRAWN));
        query.setReturned(Boolean.TRUE);
        when(taskHistoryMapper.countTasks(any())).thenReturn(0L);

        service.pageByBusinessKey(query);

        ArgumentCaptor<WorkflowTaskHistoryCriteria> captor =
                ArgumentCaptor.forClass(WorkflowTaskHistoryCriteria.class);
        verify(taskHistoryMapper).countTasks(captor.capture());
        WorkflowTaskHistoryCriteria criteria = captor.getValue();
        assertThat(criteria.isIncludeTodo()).isTrue();
        assertThat(criteria.isIncludeDone()).isFalse();
        assertThat(criteria.isIncludeCanceled()).isFalse();
        assertThat(criteria.getAssigneeRequired()).isFalse();
        assertThat(criteria.getApplyStatuses()).containsExactly("WITHDRAWN");
        assertThat(criteria.getReturned()).isTrue();
        assertThat(criteria.getKeyword()).isNull();
    }

    @Test
    void honoursUnpagedLimitAndPagedOffset() {
        WorkflowTaskBusinessPageQuery unpaged = query(WorkflowTaskQueryScope.SELF);
        when(taskHistoryMapper.countTasks(any())).thenReturn(3000L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong())).thenReturn(List.of());

        PageResult<WorkflowTaskVO> unpagedResult = service.pageByBusinessKey(unpaged);

        verify(taskHistoryMapper).selectTasks(any(), eq(2000), eq(0L));
        assertThat(unpagedResult.getTotal()).isEqualTo(3000L);
        assertThat(unpagedResult.getSize()).isZero();

        WorkflowTaskBusinessPageQuery paged = query(WorkflowTaskQueryScope.SELF);
        paged.setPage(3);
        paged.setSize(20);
        when(taskHistoryMapper.countTasks(any())).thenReturn(50L);

        PageResult<WorkflowTaskVO> pagedResult = service.pageByBusinessKey(paged);

        verify(taskHistoryMapper).selectTasks(any(), eq(20), eq(40L));
        assertThat(pagedResult.getPage()).isEqualTo(3);
        assertThat(pagedResult.getSize()).isEqualTo(20);
        assertThat(pagedResult.getTotal()).isEqualTo(50L);
    }

    @Test
    void keepsNullEndTimeAndCanceledReasonInResponse() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        WorkflowTaskHistoryRow canceled = row();
        canceled.setEndTime(null);
        canceled.setDeleteReason("审批驳回");
        when(taskHistoryMapper.countTasks(any())).thenReturn(1L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong())).thenReturn(List.of(canceled));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        WorkflowTaskVO vo = result.getList().get(0);
        assertThat(vo.getStatus()).isEqualTo(WorkflowTaskRuntimeStatus.CANCELED.name());
        assertThat(vo.getDeleteReason()).isEqualTo("审批驳回");
        assertThat(vo.getEndTime()).isNull();
        assertThat(vo.getReturned()).isTrue();
    }

    @Test
    void resolvesCancelCauseAndReturnTargetForCanceledTask() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        WorkflowTaskHistoryRow returned = row();
        returned.setEndTime(null);
        returned.setDeleteReason("Change activity to risk_specialist_review");
        returned.setApplyStatus("IN_APPROVAL");
        returned.setTaskReturned(Boolean.TRUE);
        returned.setLastReturnAt(LocalDateTime.of(2026, 1, 2, 9, 0));
        returned.setLastReturnVariables("{\"targetTaskDefinitionKey\":\"risk_specialist_review\"}");
        WorkflowTaskHistoryRow withdrawn = row();
        withdrawn.setEndTime(null);
        withdrawn.setDeleteReason("申请人撤回");
        withdrawn.setApplyStatus("WITHDRAWN");
        withdrawn.setTaskReturned(Boolean.FALSE);
        when(taskHistoryMapper.countTasks(any())).thenReturn(2L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong()))
                .thenReturn(List.of(returned, withdrawn));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        WorkflowTaskVO returnedVo = result.getList().get(0);
        assertThat(returnedVo.getStatus()).isEqualTo(WorkflowTaskRuntimeStatus.CANCELED.name());
        assertThat(returnedVo.getCancelCause()).isEqualTo(WorkflowTaskCancelCause.RETURNED.name());
        assertThat(returnedVo.getLastReturnAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 0));
        assertThat(returnedVo.getLastReturnTarget()).isEqualTo("risk_specialist_review");
        WorkflowTaskVO withdrawnVo = result.getList().get(1);
        assertThat(withdrawnVo.getCancelCause()).isEqualTo(WorkflowTaskCancelCause.WITHDRAWN.name());
        assertThat(withdrawnVo.getLastReturnTarget()).isNull();
    }

    @Test
    void resolvesTerminatedAndOtherCancelCause() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        WorkflowTaskHistoryRow terminated = row();
        terminated.setEndTime(null);
        terminated.setDeleteReason("审批人为空，系统自动结束");
        terminated.setApplyStatus("TERMINATED");
        terminated.setTaskReturned(Boolean.FALSE);
        WorkflowTaskHistoryRow rejected = row();
        rejected.setEndTime(null);
        rejected.setDeleteReason("审批驳回");
        rejected.setApplyStatus("REJECTED");
        rejected.setTaskReturned(Boolean.FALSE);
        when(taskHistoryMapper.countTasks(any())).thenReturn(2L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong()))
                .thenReturn(List.of(terminated, rejected));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        assertThat(result.getList().get(0).getCancelCause())
                .isEqualTo(WorkflowTaskCancelCause.TERMINATED.name());
        assertThat(result.getList().get(1).getCancelCause())
                .isEqualTo(WorkflowTaskCancelCause.OTHER.name());
    }

    @Test
    void keepsCancelCauseNullForCompletedTask() {
        WorkflowTaskBusinessPageQuery query = query(WorkflowTaskQueryScope.SELF);
        when(taskHistoryMapper.countTasks(any())).thenReturn(1L);
        when(taskHistoryMapper.selectTasks(any(), anyInt(), anyLong())).thenReturn(List.of(row()));

        PageResult<WorkflowTaskVO> result = service.pageByBusinessKey(query);

        assertThat(result.getList().get(0).getCancelCause()).isNull();
        assertThat(result.getList().get(0).getStatus()).isEqualTo(WorkflowTaskRuntimeStatus.DONE.name());
    }


    @Test
    void traceRejectsBlankBusinessKey() {
        WorkflowTaskTraceQuery query = traceQuery();
        query.setBusinessKey("  ");

        assertThatThrownBy(() -> service.traceByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("业务主键不能为空");
        verify(taskRecordMapper, never()).selectList(any());
    }

    @Test
    void traceRejectsUnknownAction() {
        WorkflowTaskTraceQuery query = traceQuery();
        query.setActions(List.of("COMPLETE", "RETRUN"));

        assertThatThrownBy(() -> service.traceByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未知审批动作：RETRUN");
        verify(taskRecordMapper, never()).selectList(any());
    }

    @Test
    void traceRejectsInvalidTimeRange() {
        WorkflowTaskTraceQuery query = traceQuery();
        query.setBeginTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> service.traceByBusinessKey(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("时间区间非法");
    }

    @Test
    void traceReturnsEmptyWhenBusinessKeyHasNoProcessInstance() {
        when(businessApplyMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.traceByBusinessKey(traceQuery())).isEmpty();
        verify(taskRecordMapper, never()).selectList(any());
    }

    @Test
    void traceFailsInsteadOfTruncatingWhenRecordCountExceedsLimit() {
        properties.setUnpagedMaxRows(2);
        when(businessApplyMapper.selectList(any())).thenReturn(List.of(apply("P1")));
        when(taskRecordMapper.selectList(any())).thenReturn(List.of(record(1L, "P1"), record(2L, "P1"),
                record(3L, "P1")));

        assertThatThrownBy(() -> service.traceByBusinessKey(traceQuery()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("单次返回上限");
    }

    @Test
    void traceReturnsRecordsWithoutVariables() {
        when(businessApplyMapper.selectList(any())).thenReturn(List.of(apply("P1")));
        when(taskRecordMapper.selectList(any())).thenReturn(List.of(record(1L, "P1"), record(2L, "P1")));

        List<WorkflowTaskRecordVO> records = service.traceByBusinessKey(traceQuery());

        assertThat(records).extracting(WorkflowTaskRecordVO::getId).containsExactly(1L, 2L);
        assertThat(records).allSatisfy(record -> assertThat(record.getVariables()).isNull());
        assertThat(records.get(0).getActionName()).isEqualTo("发起");
    }

    @Test
    void traceResolvesOperatorNamesBeforeFiltering() {
        when(businessApplyMapper.selectList(any())).thenReturn(List.of(apply("P1")));
        when(taskHistoryMapper.selectUserNamesByIds(anyList(), eq("1"))).thenReturn(List.of("alice"));
        when(taskRecordMapper.selectList(any())).thenReturn(List.of(record(1L, "P1")));
        WorkflowTaskTraceQuery query = traceQuery();
        query.setOperatorIds(List.of(1001L));

        assertThat(service.traceByBusinessKey(query)).hasSize(1);
        verify(taskHistoryMapper).selectUserNamesByIds(List.of("1001"), "1");
    }

    private WorkflowTaskTraceQuery traceQuery() {
        WorkflowTaskTraceQuery query = new WorkflowTaskTraceQuery();
        query.setBusinessType("BAOHAN_RC_PROJECT_REVIEW");
        query.setBusinessKey("RC1");
        return query;
    }

    private WorkflowBusinessApplyEntity apply(String processInstanceId) {
        WorkflowBusinessApplyEntity apply = new WorkflowBusinessApplyEntity();
        apply.setProcessInstanceId(processInstanceId);
        return apply;
    }

    private WorkflowTaskRecordEntity record(Long id, String processInstanceId) {
        WorkflowTaskRecordEntity record = new WorkflowTaskRecordEntity();
        record.setId(id);
        record.setProcessInstanceId(processInstanceId);
        record.setAction("START");
        record.setOperatorId(1001L);
        record.setOperatorName("alice");
        record.setCreatedTime(LocalDateTime.of(2026, 1, 1, 10, 0).plusMinutes(id));
        return record;
    }

    private WorkflowTaskBusinessPageQuery query(WorkflowTaskQueryScope scope) {
        WorkflowTaskBusinessPageQuery query = new WorkflowTaskBusinessPageQuery();
        query.setBusinessType("BAOHAN_RC_PROJECT_REVIEW");
        query.setBusinessKeys(List.of("RC1"));
        query.setScope(scope);
        return query;
    }

    private WorkflowTaskHistoryRow row() {
        WorkflowTaskHistoryRow row = new WorkflowTaskHistoryRow();
        row.setTaskId("T1");
        row.setTaskName("初审");
        row.setTaskDefinitionKey("FIRST_REVIEW");
        row.setProcessInstanceId("P1");
        row.setAssigneeKey("alice");
        row.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        row.setClaimTime(LocalDateTime.of(2026, 1, 1, 10, 0, 30));
        row.setEndTime(LocalDateTime.of(2026, 1, 1, 11, 0));
        row.setApplyId(10L);
        row.setBusinessKey("RC1");
        row.setApplyStatus("IN_APPROVAL");
        row.setProcessName("项目审批");
        row.setProcessKey("rc_project_review");
        row.setProcessDefinitionId("PD1");
        row.setOrgId(200L);
        row.setReturned(Boolean.TRUE);
        return row;
    }
}
