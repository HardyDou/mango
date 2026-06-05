package io.mango.job.starter.powerjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import org.springframework.util.StringUtils;

/**
 * PowerJob 与 Mango Job 桥接参数。
 */
final class PowerJobMangoPayload {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PowerJobMangoPayload() {
    }

    static String jobParams(MangoJobDefinitionEntity definition) {
        JobParams params = new JobParams();
        params.setTenantId(definition.getTenantId());
        params.setAppCode(definition.getAppCode());
        params.setJobCode(definition.getJobCode());
        params.setHandlerName(definition.getHandlerName());
        params.setParameter(definition.getParamValue());
        return write(params);
    }

    static String instanceParams(MangoJobDefinitionEntity definition,
                                 MangoJobInstanceEntity instance,
                                 String triggerBatchNo,
                                 String triggerParameter) {
        InstanceParams params = new InstanceParams();
        params.setMangoInstanceId(instance == null ? null : instance.getId());
        params.setTriggerBatchNo(triggerBatchNo);
        params.setParameter(StringUtils.hasText(triggerParameter) ? triggerParameter : definition.getParamValue());
        return write(params);
    }

    static JobParams readJobParams(String value) {
        if (!StringUtils.hasText(value)) {
            return new JobParams();
        }
        return read(value, JobParams.class);
    }

    static InstanceParams readInstanceParams(String value) {
        if (!StringUtils.hasText(value)) {
            return new InstanceParams();
        }
        return read(value, InstanceParams.class);
    }

    private static String write(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("PowerJob Mango 参数序列化失败", ex);
        }
    }

    private static <T> T read(String value, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("PowerJob Mango 参数解析失败", ex);
        }
    }

    static class JobParams {

        private String tenantId;

        private String appCode;

        private String jobCode;

        private String handlerName;

        private String parameter;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        public String getJobCode() {
            return jobCode;
        }

        public void setJobCode(String jobCode) {
            this.jobCode = jobCode;
        }

        public String getHandlerName() {
            return handlerName;
        }

        public void setHandlerName(String handlerName) {
            this.handlerName = handlerName;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }
    }

    static class InstanceParams {

        private Long mangoInstanceId;

        private String triggerBatchNo;

        private String parameter;

        public Long getMangoInstanceId() {
            return mangoInstanceId;
        }

        public void setMangoInstanceId(Long mangoInstanceId) {
            this.mangoInstanceId = mangoInstanceId;
        }

        public String getTriggerBatchNo() {
            return triggerBatchNo;
        }

        public void setTriggerBatchNo(String triggerBatchNo) {
            this.triggerBatchNo = triggerBatchNo;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }
    }
}
