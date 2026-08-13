package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.query.NoticeInboundMessagePageQuery;
import io.mango.notice.api.vo.NoticeInboundMessageVO;

public interface INoticeInboundQueryService {

    PageResult<NoticeInboundMessageVO> listInboundMessages(NoticeInboundMessagePageQuery query);

    NoticeInboundMessageVO getInboundMessage(Long id);
}
