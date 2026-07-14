package io.mango.notice.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.CompleteNoticeSiteMessageActionCommand;
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
import io.mango.notice.api.vo.NoticeSiteMessageActionRequestVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.api.vo.WecomUserSyncResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface NoticeApi {

    R<NoticeSendResultVO> send(@Valid SendNoticeCommand command);

    R<NoticeSendResultVO> sendSiteMessage(@Valid SendNoticeCommand command);

    R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(@Valid NoticeBusinessTypePageQuery query);

    R<NoticeBusinessTypeVO> createBusinessType(@Valid CreateNoticeBusinessTypeCommand command);

    R<NoticeBusinessTypeVO> updateBusinessType(@Positive Long id, @Valid UpdateNoticeBusinessTypeCommand command);

    R<Boolean> deleteBusinessType(@Positive Long id);

    R<Boolean> enableBusinessType(@Positive Long id);

    R<Boolean> disableBusinessType(@Positive Long id);

    R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(@Positive Long businessTypeId);

    R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(@Positive Long businessTypeId,
            @Valid SaveNoticeBusinessConfigCommand command);

    R<Boolean> publishBusinessConfigDraft(@Positive Long businessTypeId);

    R<Boolean> activateBusinessConfigVersion(@Positive Long businessTypeId, @Positive Integer version);

    R<List<NoticeChannelTemplateVO>> listChannelTemplates(@Positive Long businessTypeId);

    R<NoticeChannelTemplateVO> saveChannelTemplate(@Positive Long businessTypeId,
            @Valid SaveNoticeChannelTemplateCommand command);

    R<Boolean> publishChannelTemplate(@Positive Long businessTypeId, @NotNull NoticeChannelType channelType);

    R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(@Valid NoticeChannelConfigPageQuery query);

    R<NoticeChannelConfigVO> saveChannelConfig(@Valid SaveNoticeChannelConfigCommand command);

    R<NoticeWecomLoginConfigVO> getWecomLoginConfig(@Positive Long channelConfigId);

    R<Boolean> deleteChannelConfig(@Positive Long id);

    R<PageResult<NoticeTaskVO>> listTasks(@Valid NoticeTaskPageQuery query);

    R<PageResult<NoticeSendRecordVO>> listSendRecords(@Valid NoticeSendRecordPageQuery query);

    R<Boolean> retrySendRecord(@Positive Long id);

    R<Boolean> retrySendRecords(@Valid RetryNoticeSendRecordsCommand command);

    R<Boolean> markSendRecordManualSuccess(@Positive Long id, @Valid HandleNoticeSendRecordCommand command);

    R<Boolean> markSendRecordsManualSuccess(@Valid HandleNoticeSendRecordsCommand command);

    R<Boolean> ignoreSendRecord(@Positive Long id, @Valid HandleNoticeSendRecordCommand command);

    R<Boolean> ignoreSendRecords(@Valid HandleNoticeSendRecordsCommand command);

    R<NoticeSettingsVO> getSettings();

    R<Boolean> saveSettings(@Valid SaveNoticeSettingsCommand command);

    R<List<NoticeRecipientAccountVO>> listRecipientAccounts(@Valid NoticeRecipientAccountQuery query);

    R<NoticeRecipientAccountVO> saveRecipientAccount(@Valid SaveNoticeRecipientAccountCommand command);

    R<WecomUserSyncResultVO> syncWecomUsers(@Valid SyncWecomUsersCommand command);

    R<Boolean> disableRecipientAccount(@Positive Long id, @Positive Long userId);

    R<Boolean> setDefaultRecipientAccount(@Positive Long id, @Positive Long userId);

    R<List<NoticeReceivePreferenceVO>> listReceivePreferences(@Valid NoticeReceivePreferenceQuery query);

    R<NoticeReceivePreferenceVO> saveReceivePreference(@Valid SaveNoticeReceivePreferenceCommand command);

    R<PageResult<NoticeSiteMessageVO>> listSiteMessages(@Valid NoticeSiteMessagePageQuery query);

    R<NoticeSiteMessageVO> getSiteMessage(@Positive Long id);

    R<NoticeSiteMessageActionRequestVO> executeSiteMessageAction(
            @Valid ExecuteNoticeSiteMessageActionCommand command);

    R<NoticeSiteMessageActionRequestVO> completeSiteMessageAction(
            @Valid CompleteNoticeSiteMessageActionCommand command);

    R<NoticeUnreadCountVO> unreadCount();

    R<Boolean> markSiteMessageRead(@Positive Long id);

    R<Boolean> markSiteMessagesRead(@Valid MarkNoticeReadCommand command);

    R<Boolean> markAllSiteMessagesRead();

    R<Boolean> deleteSiteMessage(@Positive Long id);
}
