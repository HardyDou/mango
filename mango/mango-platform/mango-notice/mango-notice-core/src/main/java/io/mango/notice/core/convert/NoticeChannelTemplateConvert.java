package io.mango.notice.core.convert;

import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;

public final class NoticeChannelTemplateConvert {

 private NoticeChannelTemplateConvert() {
 }

 public static NoticeChannelTemplateVO toVO(NoticeBusinessChannelTemplateEntity entity) {
 NoticeChannelTemplateVO vo = new NoticeChannelTemplateVO();
 vo.setId(entity.getId());
 vo.setBusinessTypeId(entity.getBusinessTypeId());
 vo.setBizType(entity.getBizType());
 vo.setChannelType(entity.getChannelType());
 vo.setTemplateName(entity.getTemplateName());
 vo.setTitleTemplate(entity.getTitleTemplate());
 vo.setContentTemplate(entity.getContentTemplate());
 vo.setChannelTemplateId(entity.getChannelTemplateId());
 vo.setVariableMapping(entity.getVariableMapping());
 vo.setVersion(entity.getVersion());
 vo.setVersionStatus(entity.getVersionStatus());
 vo.setPublishTime(entity.getPublishTime());
 vo.setEnabled(entity.getEnabled());
 vo.setChannelConfigId(entity.getChannelConfigId());
 return vo;
 }
}
