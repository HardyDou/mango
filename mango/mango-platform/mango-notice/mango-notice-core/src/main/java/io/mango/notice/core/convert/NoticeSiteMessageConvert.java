package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;

public final class NoticeSiteMessageConvert {

    private NoticeSiteMessageConvert() {
    }

    public static NoticeSiteMessageVO toVO(NoticeSiteMessageEntity entity) {
        NoticeSiteMessageVO vo = new NoticeSiteMessageVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setUserId(entity.getUserId());
        vo.setPriority(entity.getPriority());
        vo.setReadStatus(entity.getReadStatus());
        vo.setReadTime(entity.getReadTime());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setCreateTime(entity.getCreatedAt());
        return vo;
    }
}
