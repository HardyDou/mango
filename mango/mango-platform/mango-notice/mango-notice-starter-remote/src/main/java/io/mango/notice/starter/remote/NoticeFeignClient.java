package io.mango.notice.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.NoticeApi;
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
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-notice", contextId = "noticeFeignClient", path = "/notice")
public interface NoticeFeignClient extends NoticeApi {

    @Override
    @PostMapping("/send")
    R<NoticeSendResultVO> send(@RequestBody SendNoticeCommand command);

    @Override
    @PostMapping("/site/messages")
    R<NoticeSendResultVO> sendSiteMessage(@RequestBody SendNoticeCommand command);

    @Override
    @GetMapping("/business-types")
    R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(@SpringQueryMap NoticeBusinessTypePageQuery query);

    @Override
    @PostMapping("/business-types")
    R<NoticeBusinessTypeVO> createBusinessType(@RequestBody CreateNoticeBusinessTypeCommand command);

    @Override
    @PutMapping("/business-types/{id}")
    R<NoticeBusinessTypeVO> updateBusinessType(@PathVariable("id") Long id,
            @RequestBody UpdateNoticeBusinessTypeCommand command);

    @Override
    @DeleteMapping("/business-types/{id}")
    R<Boolean> deleteBusinessType(@PathVariable("id") Long id);

    @Override
    @PostMapping("/business-types/{id}/enable")
    R<Boolean> enableBusinessType(@PathVariable("id") Long id);

    @Override
    @PostMapping("/business-types/{id}/disable")
    R<Boolean> disableBusinessType(@PathVariable("id") Long id);

    @Override
    @GetMapping("/business-types/{businessTypeId}/config-versions")
    R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(
            @PathVariable("businessTypeId") Long businessTypeId);

    @Override
    @PutMapping("/business-types/{businessTypeId}/config-draft")
    R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(
            @PathVariable("businessTypeId") Long businessTypeId,
            @RequestBody SaveNoticeBusinessConfigCommand command);

    @Override
    @PostMapping("/business-types/{businessTypeId}/config-draft/publish")
    R<Boolean> publishBusinessConfigDraft(@PathVariable("businessTypeId") Long businessTypeId);

    @Override
    @PostMapping("/business-types/{businessTypeId}/config-versions/{version}/activate")
    R<Boolean> activateBusinessConfigVersion(@PathVariable("businessTypeId") Long businessTypeId,
            @PathVariable("version") Integer version);

    @Override
    @GetMapping("/business-types/{businessTypeId}/channel-templates")
    R<List<NoticeChannelTemplateVO>> listChannelTemplates(@PathVariable("businessTypeId") Long businessTypeId);

    @Override
    @PutMapping("/business-types/{businessTypeId}/channel-templates/{channelType}")
    R<NoticeChannelTemplateVO> saveChannelTemplate(@PathVariable("businessTypeId") Long businessTypeId,
            @PathVariable("channelType") NoticeChannelType channelType,
            @RequestBody SaveNoticeChannelTemplateCommand command);

    @Override
    @PostMapping("/business-types/{businessTypeId}/channel-templates/{channelType}/publish")
    R<Boolean> publishChannelTemplate(@PathVariable("businessTypeId") Long businessTypeId,
            @PathVariable("channelType") NoticeChannelType channelType);

    @Override
    @GetMapping("/channels")
    R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(@SpringQueryMap NoticeChannelConfigPageQuery query);

    @Override
    @PostMapping("/channels")
    R<NoticeChannelConfigVO> saveChannelConfig(@RequestBody SaveNoticeChannelConfigCommand command);

    @Override
    @GetMapping("/internal/wecom-login-config")
    R<NoticeWecomLoginConfigVO> getWecomLoginConfig(@RequestParam(required = false) Long channelConfigId);

    @Override
    @DeleteMapping("/channels")
    R<Boolean> deleteChannelConfig(@RequestParam("id") Long id);

    @Override
    @GetMapping("/tasks")
    R<PageResult<NoticeTaskVO>> listTasks(@SpringQueryMap NoticeTaskPageQuery query);

    @Override
    @GetMapping("/records")
    R<PageResult<NoticeSendRecordVO>> listSendRecords(@SpringQueryMap NoticeSendRecordPageQuery query);

    @Override
    @PostMapping("/records/{id}/retry")
    R<Boolean> retrySendRecord(@PathVariable("id") Long id);

    @Override
    @PostMapping("/records/retry-batch")
    R<Boolean> retrySendRecords(@RequestBody RetryNoticeSendRecordsCommand command);

    @Override
    @PostMapping("/records/{id}/manual-success")
    R<Boolean> markSendRecordManualSuccess(@PathVariable("id") Long id,
            @RequestBody HandleNoticeSendRecordCommand command);

    @Override
    @PostMapping("/records/manual-success-batch")
    R<Boolean> markSendRecordsManualSuccess(@RequestBody HandleNoticeSendRecordsCommand command);

    @Override
    @PostMapping("/records/{id}/ignore")
    R<Boolean> ignoreSendRecord(@PathVariable("id") Long id, @RequestBody HandleNoticeSendRecordCommand command);

    @Override
    @PostMapping("/records/ignore-batch")
    R<Boolean> ignoreSendRecords(@RequestBody HandleNoticeSendRecordsCommand command);

    @Override
    @GetMapping("/settings")
    R<NoticeSettingsVO> getSettings();

    @Override
    @PutMapping("/settings")
    R<Boolean> saveSettings(@RequestBody SaveNoticeSettingsCommand command);

    @Override
    @GetMapping("/recipient-accounts")
    R<List<NoticeRecipientAccountVO>> listRecipientAccounts(@SpringQueryMap NoticeRecipientAccountQuery query);

    @Override
    @PostMapping("/recipient-accounts")
    R<NoticeRecipientAccountVO> saveRecipientAccount(@RequestBody SaveNoticeRecipientAccountCommand command);

    @Override
    @PostMapping("/wecom/users/sync")
    R<WecomUserSyncResultVO> syncWecomUsers(@RequestBody SyncWecomUsersCommand command);

    @Override
    @PostMapping("/recipient-accounts/{id}/disable")
    R<Boolean> disableRecipientAccount(@PathVariable("id") Long id,
            @RequestParam(required = false) Long userId);

    @Override
    @PostMapping("/recipient-accounts/{id}/default")
    R<Boolean> setDefaultRecipientAccount(@PathVariable("id") Long id,
            @RequestParam(required = false) Long userId);

    @Override
    @GetMapping("/receive-preferences")
    R<List<NoticeReceivePreferenceVO>> listReceivePreferences(@SpringQueryMap NoticeReceivePreferenceQuery query);

    @Override
    @PutMapping("/receive-preferences")
    R<NoticeReceivePreferenceVO> saveReceivePreference(@RequestBody SaveNoticeReceivePreferenceCommand command);

    @Override
    @GetMapping("/site/my/messages")
    R<PageResult<NoticeSiteMessageVO>> listSiteMessages(@SpringQueryMap NoticeSiteMessagePageQuery query);

    @Override
    @GetMapping("/site/my/messages/{id}")
    R<NoticeSiteMessageVO> getSiteMessage(@PathVariable("id") Long id);

    @Override
    @GetMapping("/site/my/unread-count")
    R<NoticeUnreadCountVO> unreadCount();

    @Override
    @PostMapping("/site/my/messages/{id}/read")
    R<Boolean> markSiteMessageRead(@PathVariable("id") Long id);

    @Override
    @PostMapping("/site/my/messages/read-batch")
    R<Boolean> markSiteMessagesRead(@RequestBody MarkNoticeReadCommand command);

    @Override
    @PostMapping("/site/my/messages/read-all")
    R<Boolean> markAllSiteMessagesRead();

    @Override
    @PostMapping("/site/my/messages/{id}/delete")
    R<Boolean> deleteSiteMessage(@PathVariable("id") Long id);
}
