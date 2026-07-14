package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@org.apache.ibatis.annotations.Mapper
public interface NoticeAnnouncementRecipientMapper extends BaseMapper<NoticeAnnouncementRecipientEntity> {

    Page<NoticeAnnouncementRecipientEntity> selectMyAnnouncementPage(Page<NoticeAnnouncementRecipientEntity> page,
                                                                     @Param("userId") Long userId,
                                                                     @Param("unreadOnly") Boolean unreadOnly,
                                                                     @Param("pendingConfirmOnly") Boolean pendingConfirmOnly,
                                                                     @Param("keyword") String keyword,
                                                                     @Param("now") LocalDateTime now);

    Long countByAnnouncement(@Param("announcementId") Long announcementId);

    Long countReadByAnnouncement(@Param("announcementId") Long announcementId);

    Long countPendingConfirmByAnnouncement(@Param("announcementId") Long announcementId);

    Long countConfirmedByAnnouncement(@Param("announcementId") Long announcementId);
}
