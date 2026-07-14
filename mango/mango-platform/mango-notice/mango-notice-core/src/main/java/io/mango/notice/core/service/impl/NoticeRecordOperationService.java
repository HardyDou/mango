package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.RetryNoticeSendRecordsCommand;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.core.convert.NoticeSendRecordConvert;
import io.mango.notice.core.convert.NoticeTaskConvert;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
import io.mango.notice.core.entity.NoticeSendRecordEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeRecipientMapper;
import io.mango.notice.core.mapper.NoticeSendRecordMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.service.INoticeRecordOperationService;
import io.mango.notice.core.service.INoticeDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeRecordOperationService implements INoticeRecordOperationService {

 private static final List<NoticeSendStatus> RETRY_OPERABLE_STATUSES = List.of(
 NoticeSendStatus.FAILED, NoticeSendStatus.RETRY_WAITING, NoticeSendStatus.FINAL_FAILED);

 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeTaskMapper taskMapper;
 private final NoticeRecipientMapper recipientMapper;
 private final NoticeSendRecordMapper sendRecordMapper;
 private final INoticeDeliveryService deliveryService;

 @Override
 public PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query) {
 LambdaQueryWrapper<NoticeTaskEntity> wrapper = new LambdaQueryWrapper<>();
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeTaskEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeTaskEntity::getBizId, query.getBizId());
 }
 if (query.getStatus() != null) {
 wrapper.eq(NoticeTaskEntity::getStatus, query.getStatus());
 }
 wrapper.orderByDesc(NoticeTaskEntity::getCreatedAt);
 Page<NoticeTaskEntity> result = taskMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(this::toTaskVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query) {
 LambdaQueryWrapper<NoticeSendRecordEntity> wrapper = new LambdaQueryWrapper<>();
 Set<String> bizTypes = sendRecordBizTypes(query);
 if (sendRecordHasBizTypeFilter(query) && bizTypes.isEmpty()) {
 return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
 }
 if (!bizTypes.isEmpty()) {
 wrapper.in(NoticeSendRecordEntity::getBizType, bizTypes);
 }
 Set<Long> recipientIds = sendRecordRecipientIds(query);
 if (StringUtils.hasText(query.getRecipientKeyword()) && recipientIds.isEmpty()) {
 return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
 }
 if (!recipientIds.isEmpty()) {
 wrapper.in(NoticeSendRecordEntity::getRecipientId, recipientIds);
 }
 if (query.getTaskId() != null) {
 wrapper.eq(NoticeSendRecordEntity::getTaskId, query.getTaskId());
 }
 if (StringUtils.hasText(query.getBizType()) && bizTypes.isEmpty()) {
 wrapper.eq(NoticeSendRecordEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeSendRecordEntity::getBizId, query.getBizId());
 }
 if (query.getChannelType() != null) {
 wrapper.eq(NoticeSendRecordEntity::getChannelType, query.getChannelType());
 }
 if (query.getStatus() != null) {
 wrapper.eq(NoticeSendRecordEntity::getStatus, query.getStatus());
 }
 if (query.getStartTime() != null) {
 wrapper.ge(NoticeSendRecordEntity::getSentAt, query.getStartTime());
 }
 if (query.getEndTime() != null) {
 wrapper.le(NoticeSendRecordEntity::getSentAt, query.getEndTime());
 }
 wrapper.orderByDesc(NoticeSendRecordEntity::getCreatedAt);
 Page<NoticeSendRecordEntity> result = sendRecordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(toSendRecordVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean retrySendRecord(Long id) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
 NoticeSendRecordEntity record = sendRecordMapper.selectById(id);
 Require.notNull(record, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录不存在");
 Require.isTrue(RETRY_OPERABLE_STATUSES.contains(record.getStatus()), NoticeCode.NOTICE_BUSINESS_ERROR, "当前状态不允许重试");
 record.setStatus(NoticeSendStatus.RETRY_WAITING);
 record.setNextRetryTime(LocalDateTime.now());
 sendRecordMapper.updateById(record);
 deliveryService.executeTask(record.getTaskId());
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean retrySendRecords(RetryNoticeSendRecordsCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "批量重试参数不能为空");
 Require.isTrue(command.getIds() != null && !command.getIds().isEmpty(), NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
 for (Long id : command.getIds()) {
 retrySendRecord(id);
 }
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean markSendRecordManualSuccess(Long id, HandleNoticeSendRecordCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录处理命令不能为空");
 return handleFailedSendRecord(id, command, NoticeSendStatus.MANUAL_SUCCESS, "人工确认成功");
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean markSendRecordsManualSuccess(HandleNoticeSendRecordsCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录批量处理命令不能为空");
 handleFailedSendRecords(command, NoticeSendStatus.MANUAL_SUCCESS, "人工确认成功");
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean ignoreSendRecord(Long id, HandleNoticeSendRecordCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录处理命令不能为空");
 return handleFailedSendRecord(id, command, NoticeSendStatus.IGNORED, "忽略失败");
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean ignoreSendRecords(HandleNoticeSendRecordsCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录批量处理命令不能为空");
 handleFailedSendRecords(command, NoticeSendStatus.IGNORED, "忽略失败");
 return true;
 }

 private void handleFailedSendRecords(HandleNoticeSendRecordsCommand command, NoticeSendStatus status,
 String operationName) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "批量处理参数不能为空");
 Require.isTrue(command.getIds() != null && !command.getIds().isEmpty(), NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
 Require.notBlank(command.getReason(), NoticeCode.NOTICE_BUSINESS_ERROR, "处理原因不能为空");
 Set<Long> taskIds = new LinkedHashSet<>();
 for (Long id : command.getIds()) {
 NoticeSendRecordEntity record = handleFailedSendRecord(id, command.getReason(), status, operationName, false);
 taskIds.add(record.getTaskId());
 }
 for (Long taskId : taskIds) {
 refreshTaskStatus(taskId);
 }
 }

 private boolean handleFailedSendRecord(Long id, HandleNoticeSendRecordCommand command, NoticeSendStatus status,
 String operationName) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "处理参数不能为空");
 Require.notBlank(command.getReason(), NoticeCode.NOTICE_BUSINESS_ERROR, "处理原因不能为空");
 handleFailedSendRecord(id, command.getReason(), status, operationName, true);
 return true;
 }

 private NoticeSendRecordEntity handleFailedSendRecord(Long id, String reason, NoticeSendStatus status,
 String operationName, boolean refreshTask) {
 NoticeSendRecordEntity record = sendRecordMapper.selectById(id);
 Require.notNull(record, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录不存在");
 Require.isTrue(RETRY_OPERABLE_STATUSES.contains(record.getStatus()), NoticeCode.NOTICE_BUSINESS_ERROR, "当前状态不允许处理");
 record.setStatus(status);
 record.setFailReason(operationName + "：" + reason);
 record.setNextRetryTime(null);
 record.setSentAt(LocalDateTime.now());
 sendRecordMapper.updateById(record);
 if (refreshTask) {
 refreshTaskStatus(record.getTaskId());
 }
 return record;
 }

 private void refreshTaskStatus(Long taskId) {
 if (taskId == null) {
 return;
 }
 NoticeTaskEntity task = taskMapper.selectById(taskId);
 if (task == null || task.getStatus() == NoticeTaskStatus.CANCELED) {
 return;
 }
 List<NoticeSendRecordEntity> records = sendRecordMapper.selectList(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId));
 int successCount = 0;
 int failCount = 0;
 for (NoticeSendRecordEntity record : records) {
 if (record.getStatus() == NoticeSendStatus.SUCCESS || record.getStatus() == NoticeSendStatus.MANUAL_SUCCESS
 || record.getStatus() == NoticeSendStatus.IGNORED || record.getStatus() == NoticeSendStatus.CANCELED) {
 successCount++;
 } else if (record.getStatus() == NoticeSendStatus.FAILED || record.getStatus() == NoticeSendStatus.RETRY_WAITING
 || record.getStatus() == NoticeSendStatus.FINAL_FAILED) {
 failCount++;
 }
 }
 task.setSuccessCount(successCount);
 task.setFailCount(failCount);
 task.setStatus(resolveTaskStatus(successCount, failCount));
 taskMapper.updateById(task);
 }

 private boolean sendRecordHasBizTypeFilter(NoticeSendRecordPageQuery query) {
 return StringUtils.hasText(query.getBizGroup()) || StringUtils.hasText(query.getMessageName());
 }

 private Set<String> sendRecordBizTypes(NoticeSendRecordPageQuery query) {
 if (!sendRecordHasBizTypeFilter(query)) {
 return Collections.emptySet();
 }
 LambdaQueryWrapper<NoticeBusinessTypeEntity> wrapper = new LambdaQueryWrapper<>();
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeBusinessTypeEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizGroup())) {
 wrapper.like(NoticeBusinessTypeEntity::getBizGroup, query.getBizGroup());
 }
 if (StringUtils.hasText(query.getMessageName())) {
 wrapper.like(NoticeBusinessTypeEntity::getBizName, query.getMessageName());
 }
 return businessTypeMapper.selectList(wrapper).stream()
 .map(NoticeBusinessTypeEntity::getBizType)
 .filter(StringUtils::hasText)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 }

 private Set<Long> sendRecordRecipientIds(NoticeSendRecordPageQuery query) {
 if (!StringUtils.hasText(query.getRecipientKeyword())) {
 return Collections.emptySet();
 }
 String keyword = query.getRecipientKeyword();
 LambdaQueryWrapper<NoticeRecipientEntity> wrapper = new LambdaQueryWrapper<NoticeRecipientEntity>()
 .like(NoticeRecipientEntity::getRecipientName, keyword)
 .or()
 .like(NoticeRecipientEntity::getMobile, keyword)
 .or()
 .like(NoticeRecipientEntity::getEmail, keyword)
 .or()
 .like(NoticeRecipientEntity::getWechatOpenid, keyword)
 .or()
 .like(NoticeRecipientEntity::getWecomUserId, keyword)
 .or()
 .like(NoticeRecipientEntity::getDingtalkUserId, keyword);
 return recipientMapper.selectList(wrapper).stream()
 .map(NoticeRecipientEntity::getId)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 }

 private List<NoticeSendRecordVO> toSendRecordVOs(List<NoticeSendRecordEntity> records) {
 if (records.isEmpty()) {
 return Collections.emptyList();
 }
 Set<String> bizTypes = records.stream()
 .map(NoticeSendRecordEntity::getBizType)
 .filter(StringUtils::hasText)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<String, NoticeBusinessTypeEntity> businessTypeMap = bizTypes.isEmpty()
 ? Collections.emptyMap()
 : businessTypeMapper.selectList(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .in(NoticeBusinessTypeEntity::getBizType, bizTypes)).stream()
 .collect(Collectors.toMap(NoticeBusinessTypeEntity::getBizType, Function.identity(), (left, right) -> left));
 Set<Long> recipientIds = records.stream()
 .map(NoticeSendRecordEntity::getRecipientId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeRecipientEntity> recipientMap = recipientIds.isEmpty()
 ? Collections.emptyMap()
 : recipientMapper.selectList(new LambdaQueryWrapper<NoticeRecipientEntity>()
 .in(NoticeRecipientEntity::getId, recipientIds)).stream()
 .collect(Collectors.toMap(NoticeRecipientEntity::getId, Function.identity(), (left, right) -> left));
 Set<Long> templateIds = records.stream()
 .map(NoticeSendRecordEntity::getBusinessChannelTemplateId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeBusinessChannelTemplateEntity> templateMap = templateIds.isEmpty()
 ? Collections.emptyMap()
 : channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .in(NoticeBusinessChannelTemplateEntity::getId, templateIds)).stream()
 .collect(Collectors.toMap(NoticeBusinessChannelTemplateEntity::getId, Function.identity(), (left, right) -> left));
 Set<Long> channelConfigIds = records.stream()
 .map(NoticeSendRecordEntity::getChannelConfigId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeChannelConfigEntity> channelConfigMap = channelConfigIds.isEmpty()
 ? Collections.emptyMap()
 : channelConfigMapper.selectList(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .in(NoticeChannelConfigEntity::getId, channelConfigIds)).stream()
 .collect(Collectors.toMap(NoticeChannelConfigEntity::getId, Function.identity(), (left, right) -> left));
 return records.stream()
 .map(record -> NoticeSendRecordConvert.toVO(record, businessTypeMap.get(record.getBizType()),
 recipientMap.get(record.getRecipientId()), templateMap.get(record.getBusinessChannelTemplateId()),
 channelConfigMap.get(record.getChannelConfigId())))
 .toList();
 }

 private NoticeTaskVO toTaskVO(NoticeTaskEntity entity) {
 NoticeTaskVO vo = NoticeTaskConvert.toVO(entity);
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .eq(NoticeBusinessTypeEntity::getBizType, entity.getBizType())
 .last("limit 1"));
 if (businessType != null) {
 vo.setBizGroup(businessType.getBizGroup());
 vo.setBizName(businessType.getBizName());
 }
 return vo;
 }

 private NoticeTaskStatus resolveTaskStatus(int successCount, int failCount) {
 if (successCount > 0 && failCount == 0) {
 return NoticeTaskStatus.SUCCESS;
 }
 if (successCount > 0) {
 return NoticeTaskStatus.PARTIAL_SUCCESS;
 }
 return NoticeTaskStatus.FAILED;
 }

}
