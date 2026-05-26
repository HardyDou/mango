package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.core.entity.NoticeSendRecordEntity;

public final class NoticeSendRecordConvert {

 private NoticeSendRecordConvert() {
 }

 public static NoticeSendRecordVO toVO(NoticeSendRecordEntity entity) {
 NoticeSendRecordVO vo = new NoticeSendRecordVO();
 vo.setId(entity.getId());
 vo.setTaskId(entity.getTaskId());
 vo.setRecipientId(entity.getRecipientId());
 vo.setBizType(entity.getBizType());
 vo.setBizId(entity.getBizId());
 vo.setChannelType(entity.getChannelType());
 vo.setRequestId(entity.getRequestId());
 vo.setStatus(entity.getStatus());
 vo.setRenderedTitle(entity.getRenderedTitle());
 vo.setRenderedContent(entity.getRenderedContent());
 vo.setRequestSnapshot(entity.getRequestSnapshot());
 vo.setResponseSnapshot(entity.getResponseSnapshot());
 vo.setProviderMessageId(entity.getProviderMessageId());
 vo.setFailCode(entity.getFailCode());
 vo.setFailReason(entity.getFailReason());
 vo.setRetryCount(entity.getRetryCount());
 vo.setSentAt(entity.getSentAt());
 return vo;
 }
}
