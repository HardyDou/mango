package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎日志查询请求。
 */
@Getter
@Setter
public class MangoJobLogRequest {

    private MangoJobLogIndexEntity logIndex;

    private MangoJobInstanceEntity instance;

    private MangoJobDefinitionEntity definition;
}
