package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SaveNoticeAnnouncementCommand;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.query.NoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementStatsVO;
import io.mango.notice.api.vo.NoticeAnnouncementVO;

public interface INoticeAnnouncementService {

    PageResult<NoticeAnnouncementVO> pageAnnouncements(NoticeAnnouncementPageQuery query);

    NoticeAnnouncementVO getAnnouncement(Long id);

    NoticeAnnouncementVO createAnnouncement(SaveNoticeAnnouncementCommand command);

    NoticeAnnouncementVO updateAnnouncement(Long id, SaveNoticeAnnouncementCommand command);

    boolean publishAnnouncement(Long id, PublishNoticeAnnouncementCommand command);

    boolean offlineAnnouncement(Long id);

    NoticeAnnouncementStatsVO getAnnouncementStats(Long id);

    PageResult<NoticeAnnouncementVO> pageMyAnnouncements(Long userId, MyNoticeAnnouncementPageQuery query);

    NoticeAnnouncementVO getMyAnnouncement(Long id, Long userId);

    boolean confirmMyAnnouncement(Long id, Long userId);
}
