package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeAnnouncementStatsVO;
import io.mango.notice.api.vo.NoticeAnnouncementTargetVO;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import io.mango.notice.core.entity.NoticeAnnouncementEntity;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import io.mango.notice.core.entity.NoticeAnnouncementTargetEntity;

import java.util.List;

public final class NoticeAnnouncementConvert {

    private NoticeAnnouncementConvert() {
    }

    public static NoticeAnnouncementVO toVO(NoticeAnnouncementEntity entity) {
        NoticeAnnouncementVO vo = new NoticeAnnouncementVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setPublishTime(entity.getPublishTime());
        vo.setValidStartTime(entity.getValidStartTime());
        vo.setValidEndTime(entity.getValidEndTime());
        vo.setPinned(entity.getPinned());
        vo.setConfirmRequired(entity.getConfirmRequired());
        vo.setSyncMessageEnabled(entity.getSyncMessageEnabled());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    public static NoticeAnnouncementVO toVO(NoticeAnnouncementEntity entity,
            List<NoticeAnnouncementTargetVO> targets, NoticeAnnouncementStatsVO stats) {
        NoticeAnnouncementVO vo = toVO(entity);
        vo.setTargets(targets);
        vo.setStats(stats);
        return vo;
    }

    public static NoticeAnnouncementVO toMyVO(NoticeAnnouncementEntity entity,
            NoticeAnnouncementRecipientEntity recipient) {
        NoticeAnnouncementVO vo = toVO(entity);
        vo.setReadStatus(recipient.getReadStatus());
        vo.setReadTime(recipient.getReadTime());
        vo.setConfirmStatus(recipient.getConfirmStatus());
        vo.setConfirmTime(recipient.getConfirmTime());
        return vo;
    }

    public static NoticeAnnouncementTargetVO toTargetVO(NoticeAnnouncementTargetEntity entity) {
        NoticeAnnouncementTargetVO vo = new NoticeAnnouncementTargetVO();
        vo.setId(entity.getId());
        vo.setAnnouncementId(entity.getAnnouncementId());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setTargetName(entity.getTargetName());
        vo.setIncludeChildren(entity.getIncludeChildren());
        return vo;
    }
}
