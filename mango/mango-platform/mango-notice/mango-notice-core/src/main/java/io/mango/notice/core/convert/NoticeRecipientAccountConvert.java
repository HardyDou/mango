package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.core.entity.NoticeRecipientAccountEntity;

public final class NoticeRecipientAccountConvert {

    private NoticeRecipientAccountConvert() {
    }

    public static NoticeRecipientAccountVO toVO(NoticeRecipientAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        NoticeRecipientAccountVO vo = new NoticeRecipientAccountVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setAccountType(entity.getAccountType());
        vo.setAccountValue(entity.getAccountValue());
        vo.setDisplayName(entity.getDisplayName());
        vo.setVerifiedStatus(entity.getVerifiedStatus());
        vo.setDefaultAccount(entity.getDefaultAccount());
        vo.setEnabled(entity.getEnabled());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
