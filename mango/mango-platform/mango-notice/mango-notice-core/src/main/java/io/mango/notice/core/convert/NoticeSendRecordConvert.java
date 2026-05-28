package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
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
 vo.setBusinessChannelTemplateId(entity.getBusinessChannelTemplateId());
 vo.setTemplateVersion(entity.getTemplateVersion());
 vo.setChannelType(entity.getChannelType());
 vo.setChannelConfigId(entity.getChannelConfigId());
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

 public static NoticeSendRecordVO toVO(NoticeSendRecordEntity entity, NoticeBusinessTypeEntity businessType,
 NoticeRecipientEntity recipient, NoticeBusinessChannelTemplateEntity template, NoticeChannelConfigEntity channelConfig) {
 NoticeSendRecordVO vo = toVO(entity);
 if (businessType != null) {
 vo.setBizGroup(businessType.getBizGroup());
 vo.setMessageName(businessType.getBizName());
 }
 if (recipient != null) {
 vo.setUserId(recipient.getUserId());
 vo.setRecipientName(recipient.getRecipientName());
 vo.setRecipientAccount(recipientAccount(entity, recipient));
 }
 if (template != null) {
 vo.setBusinessChannelTemplateName(template.getTemplateName());
 }
 if (channelConfig != null) {
 vo.setChannelConfigName(channelConfig.getConfigName());
 }
 return vo;
 }

 private static String recipientAccount(NoticeSendRecordEntity entity, NoticeRecipientEntity recipient) {
 return switch (entity.getChannelType()) {
 case SITE -> null;
 case SMS -> recipient.getMobile();
 case EMAIL -> recipient.getEmail();
 case WECHAT_OFFICIAL -> recipient.getWechatOpenid();
 case WECOM -> recipient.getWecomUserId();
 case DINGTALK -> recipient.getDingtalkUserId();
 };
 }
}
