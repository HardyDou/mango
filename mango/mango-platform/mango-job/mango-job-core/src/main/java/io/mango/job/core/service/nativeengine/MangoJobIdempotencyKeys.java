package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.job.api.enums.JobTriggerType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Job 幂等键生成服务。
 */
public class MangoJobIdempotencyKeys {

    public String scheduled(MangoJobScheduleIdentity identity) {
        Require.notNull(identity, JobCode.JOB_INVALID, "计划任务幂等身份不能为空");
        Require.notNull(identity.jobId(), JobCode.JOB_INVALID, "任务 ID 不能为空");
        Require.notNull(identity.scheduledFireTime(), JobCode.JOB_INVALID, "计划触发时间不能为空");
        return digest(JobTriggerType.SCHEDULED.name(), String.valueOf(identity.jobId()),
                String.valueOf(identity.scheduleVersion()), identity.scheduledFireTime().toString());
    }

    public String manual(Long jobId, String triggerBatchNo) {
        return batch(JobTriggerType.MANUAL, jobId, triggerBatchNo);
    }

    public String api(Long jobId, String triggerBatchNo) {
        return batch(JobTriggerType.API, jobId, triggerBatchNo);
    }

    private String batch(JobTriggerType triggerType, Long jobId, String triggerBatchNo) {
        Require.notNull(jobId, JobCode.JOB_INVALID, "任务 ID 不能为空");
        Require.notBlank(triggerBatchNo, JobCode.JOB_INVALID, "触发批次号不能为空");
        return digest(triggerType.name(), String.valueOf(jobId), triggerBatchNo.trim());
    }

    private static String digest(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update(part.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder(64);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Require.rethrow(new IllegalStateException("当前 JVM 不支持 SHA-256", ex));
        }
    }
}
