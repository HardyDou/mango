package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;

public final class NoticeBusinessTypeConvert {

 private NoticeBusinessTypeConvert() {
 }

 public static NoticeBusinessTypeVO toVO(NoticeBusinessTypeEntity entity) {
 NoticeBusinessTypeVO vo = new NoticeBusinessTypeVO();
 vo.setId(entity.getId());
 vo.setBizType(entity.getBizType());
 vo.setBizName(entity.getBizName());
 vo.setBizGroup(entity.getBizGroup());
 vo.setDomainCode(entity.getDomainCode());
 vo.setDescription(entity.getDescription());
 vo.setParamsSchema(entity.getParamsSchema());
 vo.setEnabled(entity.getEnabled());
 vo.setDefaultPriority(entity.getDefaultPriority());
 vo.setIdempotentStrategy(entity.getIdempotentStrategy());
 vo.setCreatedAt(entity.getCreatedAt());
 vo.setUpdatedAt(entity.getUpdatedAt());
 return vo;
 }
}
