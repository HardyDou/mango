package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.RetryNoticeSendRecordsCommand;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeTaskVO;

/** Owns task/send-record queries and manual record state transitions. */
public interface INoticeRecordOperationService {

    PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query);

    PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query);

    boolean retrySendRecord(Long id);

    boolean retrySendRecords(RetryNoticeSendRecordsCommand command);

    boolean markSendRecordManualSuccess(Long id, HandleNoticeSendRecordCommand command);

    boolean markSendRecordsManualSuccess(HandleNoticeSendRecordsCommand command);

    boolean ignoreSendRecord(Long id, HandleNoticeSendRecordCommand command);

    boolean ignoreSendRecords(HandleNoticeSendRecordsCommand command);
}
