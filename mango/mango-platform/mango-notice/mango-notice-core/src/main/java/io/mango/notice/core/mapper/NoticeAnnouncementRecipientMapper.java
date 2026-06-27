package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface NoticeAnnouncementRecipientMapper extends BaseMapper<NoticeAnnouncementRecipientEntity> {

    @Select("""
            <script>
            select r.*
            from notice_announcement_recipient r
            join notice_announcement a on a.id = r.announcement_id
            where r.user_id = #{userId}
              and a.status = 'PUBLISHED'
              and (a.valid_start_time is null or a.valid_start_time &lt;= #{now})
              and (a.valid_end_time is null or a.valid_end_time &gt; #{now})
            <if test="unreadOnly != null and unreadOnly">
              and r.read_status = 'UNREAD'
            </if>
            <if test="pendingConfirmOnly != null and pendingConfirmOnly">
              and r.confirm_status = 'PENDING'
            </if>
            <if test="keyword != null and keyword != ''">
              and (a.title like concat('%', #{keyword}, '%') or a.content like concat('%', #{keyword}, '%'))
            </if>
            order by a.pinned desc, a.publish_time desc, r.created_at desc
            </script>
            """)
    Page<NoticeAnnouncementRecipientEntity> selectMyAnnouncementPage(Page<NoticeAnnouncementRecipientEntity> page,
                                                                     @Param("userId") Long userId,
                                                                     @Param("unreadOnly") Boolean unreadOnly,
                                                                     @Param("pendingConfirmOnly") Boolean pendingConfirmOnly,
                                                                     @Param("keyword") String keyword,
                                                                     @Param("now") LocalDateTime now);

    @Select("""
            select count(1)
            from notice_announcement_recipient
            where announcement_id = #{announcementId}
            """)
    Long countByAnnouncement(@Param("announcementId") Long announcementId);

    @Select("""
            select count(1)
            from notice_announcement_recipient
            where announcement_id = #{announcementId}
              and read_status = 'READ'
            """)
    Long countReadByAnnouncement(@Param("announcementId") Long announcementId);

    @Select("""
            select count(1)
            from notice_announcement_recipient
            where announcement_id = #{announcementId}
              and confirm_status = 'PENDING'
            """)
    Long countPendingConfirmByAnnouncement(@Param("announcementId") Long announcementId);

    @Select("""
            select count(1)
            from notice_announcement_recipient
            where announcement_id = #{announcementId}
              and confirm_status = 'CONFIRMED'
            """)
    Long countConfirmedByAnnouncement(@Param("announcementId") Long announcementId);
}
