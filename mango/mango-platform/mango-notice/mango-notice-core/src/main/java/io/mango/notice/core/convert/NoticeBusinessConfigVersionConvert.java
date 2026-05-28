package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;

public final class NoticeBusinessConfigVersionConvert {

 private NoticeBusinessConfigVersionConvert() {
 }

 public static NoticeBusinessConfigVersionVO toVO(NoticeBusinessConfigVersionEntity entity) {
 NoticeBusinessConfigVersionVO vo = new NoticeBusinessConfigVersionVO();
 vo.setId(entity.getId());
 vo.setBusinessTypeId(entity.getBusinessTypeId());
 vo.setBizType(entity.getBizType());
 vo.setParamsSchema(entity.getParamsSchema());
 vo.setDefaultPriority(entity.getDefaultPriority());
 vo.setIdempotentStrategy(entity.getIdempotentStrategy());
 vo.setVersion(entity.getVersion());
 vo.setVersionStatus(entity.getVersionStatus());
 vo.setPublishTime(entity.getPublishTime());
 return vo;
 }
}
