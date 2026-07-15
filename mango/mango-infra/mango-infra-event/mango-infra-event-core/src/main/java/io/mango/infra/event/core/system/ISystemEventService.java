package io.mango.infra.event.core.system;

import io.mango.common.vo.PageResult;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;

/**
 * 系统事件运维内部服务。
 */
public interface ISystemEventService {

    PageResult<SystemEventVO> page(SystemEventPageQuery query);

    SystemEventVO detail(String messageId);

    boolean reconsume(ReconsumeSystemEventCommand command);
}
