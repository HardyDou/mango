package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobAttemptStatus;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobWorkerStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mango 原生 Job 状态机。
 */
public class MangoJobStateMachine {

    private final Map<JobDefinitionStatus, Set<JobDefinitionStatus>> definitionTransitions =
            new EnumMap<>(JobDefinitionStatus.class);

    private final Map<JobInstanceStatus, Set<JobInstanceStatus>> instanceTransitions =
            new EnumMap<>(JobInstanceStatus.class);

    private final Map<JobAttemptStatus, Set<JobAttemptStatus>> attemptTransitions =
            new EnumMap<>(JobAttemptStatus.class);

    private final Map<JobWorkerStatus, Set<JobWorkerStatus>> workerTransitions =
            new EnumMap<>(JobWorkerStatus.class);

    public MangoJobStateMachine() {
        definitionTransitions.put(JobDefinitionStatus.DRAFT,
                EnumSet.of(JobDefinitionStatus.ENABLED, JobDefinitionStatus.DISABLED));
        definitionTransitions.put(JobDefinitionStatus.ENABLED,
                EnumSet.of(JobDefinitionStatus.PAUSED, JobDefinitionStatus.DISABLED));
        definitionTransitions.put(JobDefinitionStatus.PAUSED,
                EnumSet.of(JobDefinitionStatus.ENABLED, JobDefinitionStatus.DISABLED));
        definitionTransitions.put(JobDefinitionStatus.DISABLED,
                EnumSet.of(JobDefinitionStatus.ENABLED));

        instanceTransitions.put(JobInstanceStatus.CREATED, EnumSet.of(JobInstanceStatus.WAITING));
        instanceTransitions.put(JobInstanceStatus.WAITING,
                EnumSet.of(JobInstanceStatus.DISPATCHED, JobInstanceStatus.CANCELED));
        instanceTransitions.put(JobInstanceStatus.DISPATCHED,
                EnumSet.of(JobInstanceStatus.RUNNING, JobInstanceStatus.CANCELED, JobInstanceStatus.RETRY_WAITING));
        instanceTransitions.put(JobInstanceStatus.RUNNING,
                EnumSet.of(JobInstanceStatus.SUCCESS, JobInstanceStatus.FAILED, JobInstanceStatus.TIMEOUT,
                        JobInstanceStatus.CANCELED, JobInstanceStatus.RETRY_WAITING));
        instanceTransitions.put(JobInstanceStatus.RETRY_WAITING,
                EnumSet.of(JobInstanceStatus.WAITING, JobInstanceStatus.CANCELED, JobInstanceStatus.FAILED));

        attemptTransitions.put(JobAttemptStatus.READY, EnumSet.of(JobAttemptStatus.LEASED, JobAttemptStatus.CANCELED));
        attemptTransitions.put(JobAttemptStatus.LEASED,
                EnumSet.of(JobAttemptStatus.RUNNING, JobAttemptStatus.LOST, JobAttemptStatus.CANCELED));
        attemptTransitions.put(JobAttemptStatus.RUNNING,
                EnumSet.of(JobAttemptStatus.SUCCEEDED, JobAttemptStatus.FAILED, JobAttemptStatus.TIMED_OUT,
                        JobAttemptStatus.LOST, JobAttemptStatus.CANCELED));

        workerTransitions.put(JobWorkerStatus.REGISTERED,
                EnumSet.of(JobWorkerStatus.ONLINE, JobWorkerStatus.DISABLED, JobWorkerStatus.EXPIRED));
        workerTransitions.put(JobWorkerStatus.ONLINE,
                EnumSet.of(JobWorkerStatus.DRAINING, JobWorkerStatus.OFFLINE, JobWorkerStatus.EXPIRED,
                        JobWorkerStatus.DISABLED));
        workerTransitions.put(JobWorkerStatus.DRAINING,
                EnumSet.of(JobWorkerStatus.OFFLINE, JobWorkerStatus.EXPIRED, JobWorkerStatus.DISABLED));
        workerTransitions.put(JobWorkerStatus.OFFLINE,
                EnumSet.of(JobWorkerStatus.ONLINE, JobWorkerStatus.DISABLED));
        workerTransitions.put(JobWorkerStatus.EXPIRED,
                EnumSet.of(JobWorkerStatus.ONLINE, JobWorkerStatus.DISABLED));
    }

    public void requireDefinitionTransition(JobDefinitionStatus from, JobDefinitionStatus to) {
        requireTransition(definitionTransitions, from, to, "非法任务定义状态流转");
    }

    public void requireInstanceTransition(JobInstanceStatus from, JobInstanceStatus to) {
        requireTransition(instanceTransitions, from, to, "非法任务实例状态流转");
    }

    public void requireAttemptTransition(JobAttemptStatus from, JobAttemptStatus to) {
        requireTransition(attemptTransitions, from, to, "非法执行尝试状态流转");
    }

    public void requireWorkerTransition(JobWorkerStatus from, JobWorkerStatus to) {
        requireTransition(workerTransitions, from, to, "非法 Worker 状态流转");
    }

    private static <E extends Enum<E>> void requireTransition(Map<E, Set<E>> transitions,
                                                             E from,
                                                             E to,
                                                             String message) {
        Require.notNull(from, message + "：原状态不能为空");
        Require.notNull(to, message + "：目标状态不能为空");
        if (from == to) {
            return;
        }
        Set<E> allowedTargets = transitions.get(from);
        if (allowedTargets == null || !allowedTargets.contains(to)) {
            Require.fail(400, message + "：" + from.name() + " -> " + to.name());
        }
    }
}
