package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 引擎侧实例导入请求。
 */
@Getter
@Setter
public class MangoJobInstanceImportCriteria {

    private MangoJobDefinitionEntity definition;

    private LocalDateTime triggerTimeStart;

    private LocalDateTime triggerTimeEnd;

    private int limit = 20;

    public static MangoJobInstanceImportCriteria of(MangoJobDefinitionEntity definition,
                                                     LocalDateTime triggerTimeStart,
                                                     LocalDateTime triggerTimeEnd,
                                                     int limit) {
        MangoJobInstanceImportCriteria criteria = new MangoJobInstanceImportCriteria();
        criteria.setDefinition(definition);
        criteria.setTriggerTimeStart(triggerTimeStart);
        criteria.setTriggerTimeEnd(triggerTimeEnd);
        criteria.setLimit(limit);
        return criteria;
    }
}
