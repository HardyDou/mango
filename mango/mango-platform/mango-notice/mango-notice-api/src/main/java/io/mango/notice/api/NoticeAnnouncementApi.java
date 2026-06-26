package io.mango.notice.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SaveNoticeAnnouncementCommand;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.query.NoticeAnnouncementIdQuery;
import io.mango.notice.api.query.NoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementStatsVO;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import jakarta.validation.Valid;

public interface NoticeAnnouncementApi {

    R<PageResult<NoticeAnnouncementVO>> pageAnnouncements(NoticeAnnouncementPageQuery query);

    R<NoticeAnnouncementVO> getAnnouncement(@Valid NoticeAnnouncementIdQuery query);

    R<NoticeAnnouncementVO> createAnnouncement(@Valid SaveNoticeAnnouncementCommand command);

    R<NoticeAnnouncementVO> updateAnnouncement(@Valid SaveNoticeAnnouncementCommand command);

    R<Boolean> publishAnnouncement(@Valid PublishNoticeAnnouncementCommand command);

    R<Boolean> offlineAnnouncement(@Valid NoticeAnnouncementIdQuery query);

    R<NoticeAnnouncementStatsVO> getAnnouncementStats(@Valid NoticeAnnouncementIdQuery query);

    R<PageResult<NoticeAnnouncementVO>> pageMyAnnouncements(MyNoticeAnnouncementPageQuery query);

    R<NoticeAnnouncementVO> getMyAnnouncement(@Valid NoticeAnnouncementIdQuery query);

    R<Boolean> confirmMyAnnouncement(@Valid NoticeAnnouncementIdQuery query);
}
