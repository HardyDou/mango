package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.mango.notice.channel.site.SiteNoticeMessageWriteResult;
import io.mango.notice.channel.site.SiteNoticeMessageWriter;
import io.mango.notice.core.entity.NoticeSiteMessageActionEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.mapper.NoticeSiteMessageActionMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.support.channel.NoticeChannelMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class NoticeSiteMessageWriterImpl implements SiteNoticeMessageWriter {

 private final NoticeSiteMessageMapper messageMapper;
 private final NoticeSiteMessageActionMapper messageActionMapper;
 private final ObjectMapper objectMapper;

 @Override
 public SiteNoticeMessageWriteResult write(NoticeChannelMessage command) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setTaskId(command.getTaskId());
 entity.setSendRecordId(command.getSendRecordId());
 entity.setUserId(command.getUserId());
 entity.setTitle(command.getTitle());
 entity.setContent(command.getContent());
 applyMessageProtocol(entity, command);
 entity.setPriority(command.getPriority() == null ? NoticePriority.NORMAL : command.getPriority());
 entity.setReadStatus(NoticeReadStatus.UNREAD);
 entity.setDeleteStatus(NoticeDeleteStatus.NORMAL);
 entity.setRevokeStatus(false);
 entity.setTopStatus(false);
 entity.setBizType(command.getBizType());
 entity.setBizId(command.getBizId());
 messageMapper.insert(entity);
 writeActions(entity, command);
 Long unreadCount = messageMapper.selectCount(new LambdaQueryWrapper<NoticeSiteMessageEntity>()
 .eq(NoticeSiteMessageEntity::getUserId, command.getUserId())
 .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD)
 .eq(NoticeSiteMessageEntity::getDeleteStatus, NoticeDeleteStatus.NORMAL));
 return new SiteNoticeMessageWriteResult(entity.getId(), unreadCount);
 }

 private void applyMessageProtocol(NoticeSiteMessageEntity entity, NoticeChannelMessage command) {
 entity.setMessageScene(command.getMessageScene());
 NoticeSiteMessageSubjectCommand subject = command.getMessageSubject();
 if (subject != null) {
 entity.setSubjectType(subject.getSubjectType());
 entity.setSubjectId(subject.getSubjectId());
 entity.setSubjectName(subject.getSubjectName());
 }
 NoticeSiteMessageTargetCommand target = command.getMessageTarget();
 if (target != null) {
 entity.setTargetType(target.getTargetType());
 entity.setTargetKey(target.getTargetKey());
 entity.setTargetParamsJson(toJson(target.getParams()));
 entity.setTargetOpenMode(target.getOpenMode());
 }
 entity.setDataJson(toJson(command.getMessageData()));
 entity.setExpireTime(command.getMessageExpireTime());
 }

 private void writeActions(NoticeSiteMessageEntity message, NoticeChannelMessage command) {
 if (command.getMessageActions() == null || command.getMessageActions().isEmpty()) {
 return;
 }
 for (NoticeSiteMessageActionCommand item : command.getMessageActions()) {
 NoticeSiteMessageActionEntity action = new NoticeSiteMessageActionEntity();
 action.setMessageId(message.getId());
 action.setActionCode(item.getActionCode());
 action.setActionLabel(item.getActionLabel());
 action.setInteractionType(item.getInteractionType());
 action.setEventType(item.getEventType());
 if (item.getTarget() != null) {
 action.setTargetType(item.getTarget().getTargetType());
 action.setTargetKey(item.getTarget().getTargetKey());
 action.setTargetParamsJson(toJson(item.getTarget().getParams()));
 action.setTargetOpenMode(item.getTarget().getOpenMode());
 }
 action.setConfirmRequired(Boolean.TRUE.equals(item.getConfirmRequired()));
 action.setInputSchema(item.getInputSchema());
 action.setStatus(isExpired(message.getExpireTime()) || isExpired(item.getExpireTime())
 ? NoticeSiteMessageActionStatus.EXPIRED
 : NoticeSiteMessageActionStatus.AVAILABLE);
 action.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
 action.setExpireTime(item.getExpireTime());
 messageActionMapper.insert(action);
 }
 }

 private boolean isExpired(LocalDateTime expireTime) {
 return expireTime != null && expireTime.isBefore(LocalDateTime.now());
 }

 private String toJson(Object value) {
 try {
 return objectMapper.writeValueAsString(value == null ? Collections.emptyMap() : value);
 } catch (JsonProcessingException ex) {
 return "{}";
 }
 }
}
