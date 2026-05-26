package io.mango.notice.core.service.impl;

import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.channel.site.SiteNoticeMessageWriter;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.support.channel.ChannelSendCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeSiteMessageWriterImpl implements SiteNoticeMessageWriter {

 private final NoticeSiteMessageMapper messageMapper;

 @Override
 public Long write(ChannelSendCommand command) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setTaskId(command.getTaskId());
 entity.setSendRecordId(command.getSendRecordId());
 entity.setUserId(command.getUserId());
 entity.setTitle(command.getTitle());
 entity.setContent(command.getContent());
 entity.setPriority(command.getPriority() == null ? NoticePriority.NORMAL : command.getPriority());
 entity.setReadStatus(NoticeReadStatus.UNREAD);
 entity.setDeleteStatus(NoticeDeleteStatus.NORMAL);
 entity.setRevokeStatus(false);
 entity.setTopStatus(false);
 entity.setBizType(command.getBizType());
 entity.setBizId(command.getBizId());
 messageMapper.insert(entity);
 return entity.getId();
 }
}
