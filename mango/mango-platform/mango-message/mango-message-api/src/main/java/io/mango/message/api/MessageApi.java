package io.mango.message.api;

import io.mango.common.result.R;
import io.mango.message.api.po.SysMessagePo;
import io.mango.message.api.vo.SysMessageVO;

import java.util.List;

public interface MessageApi {

    R<Long> send(SysMessagePo po);

    R<Long> broadcast(SysMessagePo po);

    R<List<SysMessageVO>> listByUser(Long userId);

    R<Boolean> markRead(Long messageId);
}
