package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎触发请求。
 */
@Getter
@Setter
public class MangoJobTriggerRequest {

    private MangoJobDefinitionEntity definition;

    private MangoJobInstanceEntity instance;

    private String batchNo;

    private String paramValue;
}
