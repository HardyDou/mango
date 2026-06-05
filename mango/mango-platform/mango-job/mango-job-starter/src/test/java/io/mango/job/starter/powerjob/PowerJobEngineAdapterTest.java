package io.mango.job.starter.powerjob;

import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobType;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.service.engine.MangoJobEngineRequest;
import io.mango.job.core.service.engine.MangoJobEngineResult;
import io.mango.job.core.service.engine.MangoJobTriggerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.RunJobRequest;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.common.response.ResultDTO;

import static org.assertj.core.api.Assertions.assertThat;

class PowerJobEngineAdapterTest {

    private FakePowerJobClientOperations client;

    private PowerJobEngineAdapter adapter;

    @BeforeEach
    void setUp() {
        PowerJobProperties properties = new PowerJobProperties();
        properties.setAppId(10001L);
        properties.setMaxInstanceNum(3);
        properties.setConcurrency(2);
        client = new FakePowerJobClientOperations();
        adapter = new PowerJobEngineAdapter(client, properties);
    }

    @Test
    void syncDefinitionShouldSavePowerJobRequestAndEnableWhenDefinitionEnabled() {
        MangoJobDefinitionEntity definition = cronDefinition();
        client.saveJobResult = ResultDTO.success(90001L);

        MangoJobEngineResult result = adapter.syncDefinition(engineRequest(definition, "CREATE"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEngineAppId()).isEqualTo("10001");
        assertThat(result.getEngineJobId()).isEqualTo("90001");
        assertThat(client.saveJobRequest.getAppId()).isEqualTo(10001L);
        assertThat(client.saveJobRequest.getJobName()).isEqualTo("同步用户状态");
        assertThat(client.saveJobRequest.getJobDescription()).isEqualTo("sync-user-status");
        assertThat(client.saveJobRequest.getJobParams())
                .contains("\"jobCode\":\"sync-user-status\"")
                .contains("\"handlerName\":\"syncUserStatusJobHandler\"")
                .contains("\"parameter\":\"{\\\"dryRun\\\":false}\"");
        assertThat(client.saveJobRequest.getTimeExpressionType()).isEqualTo(TimeExpressionType.CRON);
        assertThat(client.saveJobRequest.getTimeExpression()).isEqualTo("0 0/5 * * * ?");
        assertThat(client.saveJobRequest.getExecuteType()).isEqualTo(ExecuteType.STANDALONE);
        assertThat(client.saveJobRequest.getProcessorType()).isEqualTo(ProcessorType.BUILT_IN);
        assertThat(client.saveJobRequest.getProcessorInfo()).isEqualTo(MangoPowerJobProcessor.PROCESSOR_NAME);
        assertThat(client.saveJobRequest.getMaxInstanceNum()).isEqualTo(3);
        assertThat(client.saveJobRequest.getConcurrency()).isEqualTo(2);
        assertThat(client.saveJobRequest.getInstanceTimeLimit()).isEqualTo(30000L);
        assertThat(client.saveJobRequest.isEnable()).isTrue();
        assertThat(client.saveJobRequest.getDispatchStrategy()).isEqualTo(DispatchStrategy.HEALTH_FIRST);
        assertThat(client.enabledJobId).isEqualTo(90001L);
        assertThat(client.disabledJobId).isNull();
    }

    @Test
    void syncDefinitionShouldCarryExistingJobIdAndDisableWhenDefinitionDisabled() {
        MangoJobDefinitionEntity definition = cronDefinition();
        definition.setEngineJobId("90002");
        definition.setStatus(JobDefinitionStatus.DISABLED.name());
        definition.setScheduleType(JobScheduleType.FIXED_RATE.name());
        definition.setScheduleExpression("30000");
        definition.setJobType(JobType.SCRIPT.name());
        definition.setHandlerName("scripts/cleanup.sh");
        client.saveJobResult = ResultDTO.success(90002L);

        MangoJobEngineResult result = adapter.syncDefinition(engineRequest(definition, "UPDATE_STATUS"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(client.saveJobRequest.getId()).isEqualTo(90002L);
        assertThat(client.saveJobRequest.getTimeExpressionType()).isEqualTo(TimeExpressionType.FIXED_RATE);
        assertThat(client.saveJobRequest.getProcessorType()).isEqualTo(ProcessorType.SHELL);
        assertThat(client.saveJobRequest.isEnable()).isFalse();
        assertThat(client.disabledJobId).isEqualTo(90002L);
        assertThat(client.enabledJobId).isNull();
    }

    @Test
    void syncDefinitionShouldReturnFailureWhenPowerJobSaveFails() {
        client.saveJobResult = ResultDTO.failed("powerjob save failed");

        MangoJobEngineResult result = adapter.syncDefinition(engineRequest(cronDefinition(), "CREATE"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorSummary()).isEqualTo("powerjob save failed");
        assertThat(client.enabledJobId).isNull();
    }

    @Test
    void triggerShouldRunPowerJobWithMangoBatchNo() {
        MangoJobDefinitionEntity definition = cronDefinition();
        definition.setEngineJobId("90003");
        client.runJobResult = PowerResultDTO.s(80001L);
        MangoJobTriggerRequest request = new MangoJobTriggerRequest();
        request.setDefinition(definition);
        request.setInstance(new io.mango.job.core.entity.MangoJobInstanceEntity());
        request.getInstance().setId(70001L);
        request.setBatchNo("batch-20260605");

        MangoJobEngineResult result = adapter.trigger(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEngineInstanceId()).isEqualTo("80001");
        assertThat(client.runJobRequest.getAppId()).isEqualTo(10001L);
        assertThat(client.runJobRequest.getJobId()).isEqualTo(90003L);
        assertThat(client.runJobRequest.getInstanceParams())
                .contains("\"mangoInstanceId\":70001")
                .contains("\"triggerBatchNo\":\"batch-20260605\"")
                .contains("\"parameter\":\"{\\\"dryRun\\\":false}\"");
        assertThat(client.runJobRequest.getOuterKey()).isEqualTo("batch-20260605");
    }

    @Test
    void triggerShouldFailWhenDefinitionHasNoEngineJobId() {
        MangoJobEngineResult result = adapter.trigger(new MangoJobTriggerRequest() {{
            setDefinition(cronDefinition());
        }});

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorSummary()).isEqualTo("任务尚未同步到 PowerJob");
        assertThat(client.runJobRequest).isNull();
    }

    @Test
    void refreshInstanceShouldMapPowerJobStatus() {
        InstanceInfoDTO info = new InstanceInfoDTO();
        info.setStatus(InstanceStatus.SUCCEED.getV());
        info.setActualTriggerTime(1000L);
        info.setFinishedTime(3500L);
        client.fetchInstanceInfoResult = ResultDTO.success(info);
        MangoJobInstanceEntity instance = new MangoJobInstanceEntity();
        instance.setEngineInstanceId("80001");
        MangoJobTriggerRequest request = new MangoJobTriggerRequest();
        request.setDefinition(cronDefinition());
        request.setInstance(instance);

        MangoJobEngineResult result = adapter.refreshInstance(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getInstanceStatus()).isEqualTo(JobInstanceStatus.SUCCESS.name());
        assertThat(result.getDurationMillis()).isEqualTo(2500L);
        assertThat(result.getErrorSummary()).isNull();
        assertThat(result.getStartTime()).isNotNull();
        assertThat(result.getEndTime()).isNotNull();
        assertThat(client.fetchInstanceInfoId).isEqualTo(80001L);
    }

    @Test
    void healthShouldExposePowerJobFailure() {
        client.fetchAllJobResult = ResultDTO.failed("powerjob unavailable");

        MangoJobEngineResult result = adapter.health();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorSummary()).isEqualTo("powerjob unavailable");
    }

    @Test
    void healthShouldExposePowerJobException() {
        client.fetchAllJobError = new IllegalStateException("powerjob auth failed");

        MangoJobEngineResult result = adapter.health();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorSummary()).isEqualTo("powerjob auth failed");
    }

    private MangoJobDefinitionEntity cronDefinition() {
        MangoJobDefinitionEntity definition = new MangoJobDefinitionEntity();
        definition.setJobCode("sync-user-status");
        definition.setJobName("同步用户状态");
        definition.setJobType(JobType.BUILTIN.name());
        definition.setScheduleType(JobScheduleType.CRON.name());
        definition.setScheduleExpression("0 0/5 * * * ?");
        definition.setHandlerName("syncUserStatusJobHandler");
        definition.setParamValue("{\"dryRun\":false}");
        definition.setTimeoutSeconds(30);
        definition.setStatus(JobDefinitionStatus.ENABLED.name());
        return definition;
    }

    private MangoJobEngineRequest engineRequest(MangoJobDefinitionEntity definition, String action) {
        MangoJobEngineRequest request = new MangoJobEngineRequest();
        request.setDefinition(definition);
        request.setAction(action);
        return request;
    }

    private static class FakePowerJobClientOperations implements IPowerJobClientOperations {

        private ResultDTO<Long> saveJobResult = ResultDTO.success(90001L);

        private ResultDTO<Void> enableJobResult = ResultDTO.success(null);

        private ResultDTO<Void> disableJobResult = ResultDTO.success(null);

        private ResultDTO<Void> deleteJobResult = ResultDTO.success(null);

        private PowerResultDTO<Long> runJobResult = PowerResultDTO.s(80001L);

        private ResultDTO<?> fetchAllJobResult = ResultDTO.success(null);

        private ResultDTO<InstanceInfoDTO> fetchInstanceInfoResult = ResultDTO.failed("instance missing");

        private RuntimeException fetchAllJobError;

        private SaveJobInfoRequest saveJobRequest;

        private RunJobRequest runJobRequest;

        private Long enabledJobId;

        private Long disabledJobId;

        private Long fetchInstanceInfoId;

        @Override
        public ResultDTO<Long> saveJob(SaveJobInfoRequest request) {
            this.saveJobRequest = request;
            return saveJobResult;
        }

        @Override
        public ResultDTO<Void> enableJob(Long jobId) {
            this.enabledJobId = jobId;
            return enableJobResult;
        }

        @Override
        public ResultDTO<Void> disableJob(Long jobId) {
            this.disabledJobId = jobId;
            return disableJobResult;
        }

        @Override
        public ResultDTO<Void> deleteJob(Long jobId) {
            return deleteJobResult;
        }

        @Override
        public PowerResultDTO<Long> runJob(RunJobRequest request) {
            this.runJobRequest = request;
            return runJobResult;
        }

        @Override
        public ResultDTO<InstanceInfoDTO> fetchInstanceInfo(Long instanceId) {
            this.fetchInstanceInfoId = instanceId;
            return fetchInstanceInfoResult;
        }

        @Override
        public ResultDTO<?> fetchAllJob() {
            if (fetchAllJobError != null) {
                throw fetchAllJobError;
            }
            return fetchAllJobResult;
        }
    }
}
