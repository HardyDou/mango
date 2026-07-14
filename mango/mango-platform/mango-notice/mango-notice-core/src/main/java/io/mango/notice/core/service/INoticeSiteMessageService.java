package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CompleteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.vo.NoticeSiteMessageActionRequestVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;

/** Current-user site message queries, state changes and controlled actions. */
public interface INoticeSiteMessageService {

    PageResult<NoticeSiteMessageVO> listSiteMessages(NoticeSiteMessagePageQuery query);

    NoticeSiteMessageVO getSiteMessage(Long id);

    NoticeUnreadCountVO unreadCount();

    boolean markSiteMessageRead(Long id);

    boolean markSiteMessagesRead(MarkNoticeReadCommand command);

    boolean markAllSiteMessagesRead();

    boolean deleteSiteMessage(Long id);

    NoticeSiteMessageActionRequestVO executeSiteMessageAction(ExecuteNoticeSiteMessageActionCommand command);

    NoticeSiteMessageActionRequestVO completeSiteMessageAction(CompleteNoticeSiteMessageActionCommand command);
}
