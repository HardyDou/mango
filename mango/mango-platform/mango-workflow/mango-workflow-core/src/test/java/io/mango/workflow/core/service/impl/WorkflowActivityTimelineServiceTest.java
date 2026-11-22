package io.mango.workflow.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.enums.WorkflowActivityKind;
import io.mango.workflow.api.query.WorkflowActivityTimelineQuery;
import io.mango.workflow.api.vo.WorkflowActivityTimelineVO;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowActivityTimelineServiceTest {

    private HistoryService historyService;
    private HistoricActivityInstanceQuery activityQuery;
    private WorkflowBusinessApplyMapper businessApplyMapper;
    private WorkflowTaskQueryProperties properties;
    private WorkflowActivityTimelineService service;

    @BeforeEach
    void setUp() {
        historyService = mock(HistoryService.class);
        activityQuery = mock(HistoricActivityInstanceQuery.class);
        businessApplyMapper = mock(WorkflowBusinessApplyMapper.class);
        properties = new WorkflowTaskQueryProperties();
        // 单元测试不启动 MyBatis-Plus，需先注册实体元信息，否则 LambdaQueryWrapper 解析列名会失败。
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                WorkflowBusinessApplyEntity.class);
        service = new WorkflowActivityTimelineService(historyService, businessApplyMapper, properties);
        MangoContextHolder.clear();
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "alice", "default", "USER", "ORG", 1001L, "token"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("有网关活动时返回 GATEWAY 行且处理人为空")
    void activityTimelineByBusinessKey_gatewayRowHasGatewayKindAndNullAssignee() {
        List<HistoricActivityInstance> rows = List.of(
                activity("sid-1", "开始", "startEvent", null, 1000L),
                activity("sid-2", "审批", "userTask", "alice", 2000L),
                activity("sid-3", "是否通过", "exclusiveGateway", null, 3000L));
        when(businessApplyMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(apply("P-1")));
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        when(activityQuery.processInstanceId(anyString())).thenReturn(activityQuery);
        when(activityQuery.list()).thenReturn(rows);

        List<WorkflowActivityTimelineVO> activities = service.activityTimelineByBusinessKey(query());

        assertThat(activities).hasSize(3);
        assertThat(activities).extracting(WorkflowActivityTimelineVO::getKind)
                .containsExactly(WorkflowActivityKind.EVENT, WorkflowActivityKind.TASK, WorkflowActivityKind.GATEWAY);
        WorkflowActivityTimelineVO gateway = activities.get(2);
        assertThat(gateway.getAssignee()).isNull();
        assertThat(gateway.getActivityType()).isEqualTo("exclusiveGateway");
        assertThat(activities.get(1).getAssignee()).isEqualTo("alice");
    }

    @Test
    @DisplayName("业务键未登记流程实例时返回空集合")
    void activityTimelineByBusinessKey_noProcessInstanceReturnsEmpty() {
        when(businessApplyMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        assertThat(service.activityTimelineByBusinessKey(query())).isEmpty();
    }

    @Test
    @DisplayName("活动数超过单次返回上限时抛 TASK_QUERY_INVALID")
    void activityTimelineByBusinessKey_overLimitFailsWithTaskQueryInvalid() {
        properties.setUnpagedMaxRows(2);
        List<HistoricActivityInstance> rows = List.of(
                activity("sid-1", "开始", "startEvent", null, 1000L),
                activity("sid-2", "审批", "userTask", "alice", 2000L),
                activity("sid-3", "结束", "endEvent", null, 3000L));
        when(businessApplyMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(apply("P-1")));
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        when(activityQuery.processInstanceId(anyString())).thenReturn(activityQuery);
        when(activityQuery.list()).thenReturn(rows);

        assertThatThrownBy(() -> service.activityTimelineByBusinessKey(query()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("活动时间轴超过单次返回上限");
    }

    private WorkflowActivityTimelineQuery query() {
        WorkflowActivityTimelineQuery query = new WorkflowActivityTimelineQuery();
        query.setBusinessType("BAOHAN_RC_PROJECT_REVIEW");
        query.setBusinessKey("RC202611130001");
        return query;
    }

    private WorkflowBusinessApplyEntity apply(String processInstanceId) {
        WorkflowBusinessApplyEntity entity = new WorkflowBusinessApplyEntity();
        entity.setTenantId("1");
        entity.setProcessInstanceId(processInstanceId);
        return entity;
    }

    private HistoricActivityInstance activity(String activityId, String name, String type, String assignee,
                                              long startOffsetMillis) {
        HistoricActivityInstance instance = mock(HistoricActivityInstance.class);
        when(instance.getActivityId()).thenReturn(activityId);
        when(instance.getActivityName()).thenReturn(name);
        when(instance.getActivityType()).thenReturn(type);
        when(instance.getAssignee()).thenReturn(assignee);
        when(instance.getStartTime()).thenReturn(new Date(1_700_000_000_000L + startOffsetMillis));
        when(instance.getEndTime()).thenReturn(new Date(1_700_000_000_000L + startOffsetMillis + 10L));
        when(instance.getDurationInMillis()).thenReturn(10L);
        return instance;
    }
}
