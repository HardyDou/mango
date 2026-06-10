package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.RetryNoticeSendRecordsCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.api.vo.WecomUserSyncResultVO;

import java.util.List;

public interface INoticeService {

 NoticeSendResultVO send(SendNoticeCommand command);

 String findTaskTenantId(Long taskId);

 int executeTask(Long taskId);

 boolean hasRetryWaitingRecords(Long taskId);

 void finalizeRetryWaitingRecords(Long taskId, String failReason);

 PageResult<NoticeBusinessTypeVO> listBusinessTypes(NoticeBusinessTypePageQuery query);

 NoticeBusinessTypeVO createBusinessType(CreateNoticeBusinessTypeCommand command);

 NoticeBusinessTypeVO updateBusinessType(Long id, UpdateNoticeBusinessTypeCommand command);

 boolean deleteBusinessType(Long id);

 boolean enableBusinessType(Long id);

 boolean disableBusinessType(Long id);

 List<NoticeBusinessConfigVersionVO> listBusinessConfigVersions(Long businessTypeId);

 NoticeBusinessConfigVersionVO saveBusinessConfigDraft(Long businessTypeId, SaveNoticeBusinessConfigCommand command);

 boolean publishBusinessConfigDraft(Long businessTypeId);

 boolean activateBusinessConfigVersion(Long businessTypeId, Integer version);

 List<NoticeChannelTemplateVO> listChannelTemplates(Long businessTypeId);

 NoticeChannelTemplateVO saveChannelTemplate(Long businessTypeId, NoticeChannelType channelType,
 SaveNoticeChannelTemplateCommand command);

 boolean publishChannelTemplate(Long businessTypeId, NoticeChannelType channelType);

 PageResult<NoticeChannelConfigVO> listChannelConfigs(NoticeChannelConfigPageQuery query);

 NoticeChannelConfigVO saveChannelConfig(SaveNoticeChannelConfigCommand command);

 NoticeWecomLoginConfigVO getWecomLoginConfig(Long channelConfigId);

 boolean deleteChannelConfig(Long id);

 PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query);

 PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query);

 boolean retrySendRecord(Long id);

 boolean retrySendRecords(RetryNoticeSendRecordsCommand command);

 boolean markSendRecordManualSuccess(Long id, HandleNoticeSendRecordCommand command);

 boolean markSendRecordsManualSuccess(HandleNoticeSendRecordsCommand command);

 boolean ignoreSendRecord(Long id, HandleNoticeSendRecordCommand command);

 boolean ignoreSendRecords(HandleNoticeSendRecordsCommand command);

 NoticeSettingsVO getSettings();

 boolean saveSettings(SaveNoticeSettingsCommand command);

 List<NoticeRecipientAccountVO> listRecipientAccounts(Long currentUserId, NoticeRecipientAccountQuery query);

 NoticeRecipientAccountVO saveRecipientAccount(Long currentUserId, SaveNoticeRecipientAccountCommand command);

 WecomUserSyncResultVO syncWecomUsers(SyncWecomUsersCommand command);

 boolean disableRecipientAccount(Long currentUserId, Long id, Long userId);

 boolean setDefaultRecipientAccount(Long currentUserId, Long id, Long userId);

 List<NoticeReceivePreferenceVO> listReceivePreferences(Long currentUserId, NoticeReceivePreferenceQuery query);

 NoticeReceivePreferenceVO saveReceivePreference(Long currentUserId, SaveNoticeReceivePreferenceCommand command);

 PageResult<NoticeSiteMessageVO> listSiteMessages(Long userId, NoticeSiteMessagePageQuery query);

 NoticeSiteMessageVO getSiteMessage(Long id, Long userId);

 NoticeUnreadCountVO unreadCount(Long userId);

 boolean markSiteMessageRead(Long id, Long userId);

 boolean markSiteMessagesRead(MarkNoticeReadCommand command, Long userId);

 boolean markAllSiteMessagesRead(Long userId);

 boolean deleteSiteMessage(Long id, Long userId);
}
