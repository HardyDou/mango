package io.mango.infra.event.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 系统事件运维 API 契约。
 */
public interface SystemEventApi {

    /**
     * 分页查询系统事件。
     */
    R<PageResult<SystemEventVO>> page(@Valid SystemEventPageQuery query);

    /**
     * 查询系统事件详情。
     */
    R<SystemEventVO> detail(@NotBlank(message = "消息 ID 不能为空") String messageId);

    /**
     * 重新投递系统事件。
     */
    R<Boolean> reconsume(@Valid ReconsumeSystemEventCommand command);
}
