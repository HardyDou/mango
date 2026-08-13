package io.mango.notice.core.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CompleteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.RetryNoticeSendRecordsCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.command.SaveNoticeRouteTagCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeChannelReferenceImpactQuery;
import io.mango.notice.api.query.NoticeInboundMessagePageQuery;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.query.NoticeRouteTagQuery;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelReferenceImpactVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeInboundMessageVO;
import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.api.vo.NoticeRouteTagVO;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.api.vo.NoticeSiteMessageActionRequestVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.api.vo.NoticeUnreadCategoryStatsVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.api.vo.WecomUserSyncResultVO;
import io.mango.notice.core.service.INoticeConfigurationService;
import io.mango.notice.core.service.INoticeDeliveryService;
import io.mango.notice.core.service.INoticeInboundQueryService;
import io.mango.notice.core.service.INoticeRecipientSettingService;
import io.mango.notice.core.service.INoticeRecordOperationService;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.core.service.INoticeSiteMessageService;
import io.mango.notice.core.service.INoticeWecomSyncService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are injected and intentionally shared")
public class NoticeService implements INoticeService {
    private final INoticeDeliveryService deliveryService;
    private final INoticeConfigurationService configurationService;
    private final INoticeRecordOperationService recordOperationService;
    private final INoticeInboundQueryService inboundQueryService;
    private final INoticeRecipientSettingService recipientSettingService;
    private final INoticeSiteMessageService siteMessageService;
    private final INoticeWecomSyncService wecomSyncService;

    @Override
    public NoticeSendResultVO send(SendNoticeCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "通知发送命令不能为空");
        return deliveryService.send(command);
    }

    @Override
    public String findTaskTenantId(Long taskId) {
        Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
        return deliveryService.findTaskTenantId(taskId);
    }

    @Override
    public int executeTask(Long taskId) {
        Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
        return deliveryService.executeTask(taskId);
    }

    @Override
    public boolean hasRetryWaitingRecords(Long taskId) {
        Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
        return deliveryService.hasRetryWaitingRecords(taskId);
    }

    @Override
    public void finalizeRetryWaitingRecords(Long taskId, String failReason) {
        Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
        deliveryService.finalizeRetryWaitingRecords(taskId, failReason);
    }

    @Override
    public PageResult<NoticeBusinessTypeVO> listBusinessTypes(NoticeBusinessTypePageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "查询条件不能为空");
        return configurationService.listBusinessTypes(query);
    }

    @Override
    public PageResult<NoticeBusinessTypeVO> listEnabledBusinessTypes(NoticeBusinessTypePageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "查询条件不能为空");
        NoticeBusinessTypePageQuery enabledQuery = new NoticeBusinessTypePageQuery();
        enabledQuery.setPageNum(query.getPageNum());
        enabledQuery.setPageSize(query.getPageSize());
        enabledQuery.setBizType(query.getBizType());
        enabledQuery.setBizGroup(query.getBizGroup());
        enabledQuery.setDomainCode(query.getDomainCode());
        enabledQuery.setEnabled(true);
        return configurationService.listBusinessTypes(enabledQuery);
    }

    @Override
    public NoticeBusinessTypeVO createBusinessType(CreateNoticeBusinessTypeCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务通知配置不能为空");
        return configurationService.createBusinessType(command);
    }

    @Override
    public NoticeBusinessTypeVO updateBusinessType(
            Long id, UpdateNoticeBusinessTypeCommand command) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务通知配置不能为空");
        return configurationService.updateBusinessType(id, command);
    }

    @Override
    public boolean deleteBusinessType(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.deleteBusinessType(id);
    }

    @Override
    public boolean enableBusinessType(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.enableBusinessType(id);
    }

    @Override
    public boolean disableBusinessType(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.disableBusinessType(id);
    }

    @Override
    public List<NoticeBusinessConfigVersionVO> listBusinessConfigVersions(Long businessTypeId) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.listBusinessConfigVersions(businessTypeId);
    }

    @Override
    public NoticeBusinessConfigVersionVO saveBusinessConfigDraft(
            Long businessTypeId, SaveNoticeBusinessConfigCommand command) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务发布配置不能为空");
        return configurationService.saveBusinessConfigDraft(businessTypeId, command);
    }

    @Override
    public boolean publishBusinessConfigDraft(Long businessTypeId) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.publishBusinessConfigDraft(businessTypeId);
    }

    @Override
    public boolean activateBusinessConfigVersion(Long businessTypeId, Integer version) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        Require.notNull(version, NoticeCode.NOTICE_BUSINESS_ERROR, "版本号不能为空");
        return configurationService.activateBusinessConfigVersion(businessTypeId, version);
    }

    @Override
    public List<NoticeChannelTemplateVO> listChannelTemplates(Long businessTypeId) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        return configurationService.listChannelTemplates(businessTypeId);
    }

    @Override
    public NoticeChannelTemplateVO saveChannelTemplate(
            Long businessTypeId, SaveNoticeChannelTemplateCommand command) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道模板不能为空");
        return configurationService.saveChannelTemplate(businessTypeId, command);
    }

    @Override
    public boolean publishChannelTemplate(Long businessTypeId, NoticeChannelType channelType) {
        Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
        Require.notNull(channelType, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
        return configurationService.publishChannelTemplate(businessTypeId, channelType);
    }

    @Override
    public PageResult<NoticeChannelConfigVO> listChannelConfigs(
            NoticeChannelConfigPageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "查询条件不能为空");
        return configurationService.listChannelConfigs(query);
    }

    @Override
    public NoticeChannelConfigVO saveChannelConfig(SaveNoticeChannelConfigCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置不能为空");
        return configurationService.saveChannelConfig(command);
    }

    @Override
    public List<NoticeRouteTagVO> listRouteTags(NoticeRouteTagQuery query) {
        return configurationService.listRouteTags(query);
    }

    @Override
    public NoticeRouteTagVO saveRouteTag(SaveNoticeRouteTagCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签不能为空");
        return configurationService.saveRouteTag(command);
    }

    @Override
    public boolean deleteRouteTag(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签 ID 不能为空");
        return configurationService.deleteRouteTag(id);
    }

    @Override
    public NoticeChannelReferenceImpactVO getChannelReferenceImpact(
            NoticeChannelReferenceImpactQuery query) {
        return configurationService.getChannelReferenceImpact(query);
    }

    @Override
    public NoticeWecomLoginConfigVO getWecomLoginConfig(Long channelConfigId) {
        return configurationService.getWecomLoginConfig(channelConfigId);
    }

    @Override
    public boolean deleteChannelConfig(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置ID不能为空");
        return configurationService.deleteChannelConfig(id);
    }

    @Override
    public PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "查询条件不能为空");
        return recordOperationService.listTasks(query);
    }

    @Override
    public PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "查询条件不能为空");
        return recordOperationService.listSendRecords(query);
    }

    @Override
    public PageResult<NoticeInboundMessageVO> listInboundMessages(NoticeInboundMessagePageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息查询条件不能为空");
        return inboundQueryService.listInboundMessages(query);
    }

    @Override
    public NoticeInboundMessageVO getInboundMessage(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息ID不能为空");
        return inboundQueryService.getInboundMessage(id);
    }

    @Override
    public boolean retrySendRecord(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
        return recordOperationService.retrySendRecord(id);
    }

    @Override
    public boolean retrySendRecords(RetryNoticeSendRecordsCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "批量重试参数不能为空");
        return recordOperationService.retrySendRecords(command);
    }

    @Override
    public boolean markSendRecordManualSuccess(Long id, HandleNoticeSendRecordCommand command) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录处理命令不能为空");
        return recordOperationService.markSendRecordManualSuccess(id, command);
    }

    @Override
    public boolean markSendRecordsManualSuccess(HandleNoticeSendRecordsCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录批量处理命令不能为空");
        return recordOperationService.markSendRecordsManualSuccess(command);
    }

    @Override
    public boolean ignoreSendRecord(Long id, HandleNoticeSendRecordCommand command) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录 ID 不能为空");
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录处理命令不能为空");
        return recordOperationService.ignoreSendRecord(id, command);
    }

    @Override
    public boolean ignoreSendRecords(HandleNoticeSendRecordsCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "发送记录批量处理命令不能为空");
        return recordOperationService.ignoreSendRecords(command);
    }

    @Override
    public NoticeSettingsVO getSettings() {
        return recipientSettingService.getSettings();
    }

    @Override
    public boolean saveSettings(SaveNoticeSettingsCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "通知设置保存命令不能为空");
        return recipientSettingService.saveSettings(command);
    }

    @Override
    public List<NoticeRecipientAccountVO> listRecipientAccounts(NoticeRecipientAccountQuery query) {
        return recipientSettingService.listRecipientAccounts(query);
    }

    @Override
    public NoticeRecipientAccountVO saveRecipientAccount(
            SaveNoticeRecipientAccountCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户不能为空");
        return recipientSettingService.saveRecipientAccount(command);
    }

    @Override
    public WecomUserSyncResultVO syncWecomUsers(SyncWecomUsersCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "同步参数不能为空");
        return wecomSyncService.syncWecomUsers(command);
    }

    @Override
    public boolean disableRecipientAccount(Long id, Long userId) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户 ID 不能为空");
        return recipientSettingService.disableRecipientAccount(id, userId);
    }

    @Override
    public boolean setDefaultRecipientAccount(Long id, Long userId) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "接收账户 ID 不能为空");
        return recipientSettingService.setDefaultRecipientAccount(id, userId);
    }

    @Override
    public List<NoticeReceivePreferenceVO> listReceivePreferences(
            NoticeReceivePreferenceQuery query) {
        return recipientSettingService.listReceivePreferences(query);
    }

    @Override
    public NoticeReceivePreferenceVO saveReceivePreference(
            SaveNoticeReceivePreferenceCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "接收偏好不能为空");
        return recipientSettingService.saveReceivePreference(command);
    }

    @Override
    public PageResult<NoticeSiteMessageVO> listSiteMessages(NoticeSiteMessagePageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息查询条件不能为空");
        return siteMessageService.listSiteMessages(query);
    }

    @Override
    public NoticeSiteMessageVO getSiteMessage(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        return siteMessageService.getSiteMessage(id);
    }

    @Override
    public NoticeUnreadCountVO unreadCount() {
        return siteMessageService.unreadCount();
    }

    @Override
    public NoticeUnreadCategoryStatsVO unreadCategoryStats() {
        return siteMessageService.unreadCategoryStats();
    }

    @Override
    public boolean markSiteMessageRead(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        return siteMessageService.markSiteMessageRead(id);
    }

    @Override
    public boolean markSiteMessagesRead(MarkNoticeReadCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "消息已读命令不能为空");
        return siteMessageService.markSiteMessagesRead(command);
    }

    @Override
    public boolean markAllSiteMessagesRead() {
        return siteMessageService.markAllSiteMessagesRead();
    }

    @Override
    public boolean deleteSiteMessage(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        return siteMessageService.deleteSiteMessage(id);
    }

    @Override
    public NoticeSiteMessageActionRequestVO executeSiteMessageAction(
            ExecuteNoticeSiteMessageActionCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作命令不能为空");
        return siteMessageService.executeSiteMessageAction(command);
    }

    @Override
    public NoticeSiteMessageActionRequestVO completeSiteMessageAction(
            CompleteNoticeSiteMessageActionCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作完成命令不能为空");
        return siteMessageService.completeSiteMessageAction(command);
    }
}
