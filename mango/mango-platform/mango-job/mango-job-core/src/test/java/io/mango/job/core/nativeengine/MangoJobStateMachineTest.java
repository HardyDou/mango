package io.mango.job.core.nativeengine;

import io.mango.job.api.enums.JobAttemptStatus;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.core.service.nativeengine.MangoJobStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoJobStateMachineTest {

    private final MangoJobStateMachine stateMachine = new MangoJobStateMachine();

    @Test
    void shouldAllowNativeDefinitionTransitions() {
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireDefinitionTransition(JobDefinitionStatus.DRAFT, JobDefinitionStatus.ENABLED));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireDefinitionTransition(JobDefinitionStatus.ENABLED, JobDefinitionStatus.PAUSED));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireDefinitionTransition(JobDefinitionStatus.PAUSED, JobDefinitionStatus.DISABLED));
    }

    @Test
    void shouldRejectInvalidDefinitionTransitions() {
        assertThatThrownBy(() ->
                stateMachine.requireDefinitionTransition(JobDefinitionStatus.DISABLED, JobDefinitionStatus.PAUSED))
                .hasMessageContaining("非法任务定义状态流转");
    }

    @Test
    void shouldAllowInstanceRetryFlow() {
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.CREATED, JobInstanceStatus.WAITING));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.WAITING, JobInstanceStatus.DISPATCHED));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.DISPATCHED, JobInstanceStatus.RUNNING));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.RUNNING, JobInstanceStatus.RETRY_WAITING));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.RETRY_WAITING, JobInstanceStatus.WAITING));
    }

    @Test
    void shouldRejectTerminalInstanceTransitions() {
        assertThatThrownBy(() ->
                stateMachine.requireInstanceTransition(JobInstanceStatus.SUCCESS, JobInstanceStatus.RUNNING))
                .hasMessageContaining("非法任务实例状态流转");
    }

    @Test
    void shouldRequireAttemptLeaseBeforeRunning() {
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireAttemptTransition(JobAttemptStatus.READY, JobAttemptStatus.LEASED));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireAttemptTransition(JobAttemptStatus.LEASED, JobAttemptStatus.RUNNING));
        assertThatThrownBy(() ->
                stateMachine.requireAttemptTransition(JobAttemptStatus.READY, JobAttemptStatus.RUNNING))
                .hasMessageContaining("非法执行尝试状态流转");
    }

    @Test
    void shouldAllowWorkerDrainAndExpire() {
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireWorkerTransition(JobWorkerStatus.REGISTERED, JobWorkerStatus.ONLINE));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireWorkerTransition(JobWorkerStatus.ONLINE, JobWorkerStatus.DRAINING));
        assertThatNoException().isThrownBy(() ->
                stateMachine.requireWorkerTransition(JobWorkerStatus.DRAINING, JobWorkerStatus.EXPIRED));
    }
}
