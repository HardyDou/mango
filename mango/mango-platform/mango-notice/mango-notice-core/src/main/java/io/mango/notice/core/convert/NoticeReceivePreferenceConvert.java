package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.core.entity.NoticeReceivePreferenceEntity;

public final class NoticeReceivePreferenceConvert {

    private NoticeReceivePreferenceConvert() {
    }

    public static NoticeReceivePreferenceVO toVO(NoticeReceivePreferenceEntity entity) {
        if (entity == null) {
            return null;
        }
        NoticeReceivePreferenceVO vo = new NoticeReceivePreferenceVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setScopeType(entity.getScopeType());
        vo.setScopeValue(entity.getScopeValue());
        vo.setChannelType(entity.getChannelType());
        vo.setEnabled(entity.getEnabled());
        vo.setAccountId(entity.getAccountId());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
