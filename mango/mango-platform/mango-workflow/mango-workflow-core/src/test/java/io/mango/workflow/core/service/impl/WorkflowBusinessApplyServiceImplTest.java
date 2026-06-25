package io.mango.workflow.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyCurrentTaskMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowBusinessApplyServiceImplTest {

    @Mock
    private WorkflowBusinessApplyMapper applyMapper;
    @Mock
    private WorkflowBusinessApplyCurrentTaskMapper currentTaskMapper;
    @Mock
    private WorkflowBusinessApplyStatusLogMapper statusLogMapper;
    @Mock
    private TaskService taskService;

    private WorkflowBusinessApplyServiceImpl service;

    @BeforeEach
    void setUp() {
        MangoContextHolder.clear();
        MockitoAnnotations.openMocks(this);
        service = new WorkflowBusinessApplyServiceImpl(
                applyMapper,
                currentTaskMapper,
                statusLogMapper,
                taskService,
                new ObjectMapper());
    }

    @Test
    void mySummary_shouldCountCurrentUserBusinessApplyStatuses() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "default", "admin", "default", "USER", "USER", 1L, "internal-admin"));
        when(applyMapper.selectCount(any())).thenReturn(3L, 5L, 2L, 1L);

        var summary = service.mySummary().getData();

        assertThat(summary.getInReview()).isEqualTo(3L);
        assertThat(summary.getCompleted()).isEqualTo(5L);
        assertThat(summary.getRejected()).isEqualTo(2L);
        assertThat(summary.getWithdrawn()).isEqualTo(1L);
    }

    @Test
    void mySummary_shouldReturnZeroWhenUserContextMissing() {
        var summary = service.mySummary().getData();

        assertThat(summary.getInReview()).isZero();
        assertThat(summary.getCompleted()).isZero();
        assertThat(summary.getRejected()).isZero();
        assertThat(summary.getWithdrawn()).isZero();
        verify(applyMapper, never()).selectCount(any());
    }
}
