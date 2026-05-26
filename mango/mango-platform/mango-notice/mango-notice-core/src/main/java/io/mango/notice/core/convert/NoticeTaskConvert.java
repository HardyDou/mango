package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.core.entity.NoticeTaskEntity;

public final class NoticeTaskConvert {

 private NoticeTaskConvert() {
 }

 public static NoticeTaskVO toVO(NoticeTaskEntity entity) {
 NoticeTaskVO vo = new NoticeTaskVO();
 vo.setId(entity.getId());
 vo.setTaskCode(entity.getTaskCode());
 vo.setBizType(entity.getBizType());
 vo.setBizId(entity.getBizId());
 vo.setChannelTypes(entity.getChannelTypes());
 vo.setStatus(entity.getStatus());
 vo.setTotalCount(entity.getTotalCount());
 vo.setSuccessCount(entity.getSuccessCount());
 vo.setFailCount(entity.getFailCount());
 vo.setCreatedAt(entity.getCreatedAt());
 return vo;
 }
}
