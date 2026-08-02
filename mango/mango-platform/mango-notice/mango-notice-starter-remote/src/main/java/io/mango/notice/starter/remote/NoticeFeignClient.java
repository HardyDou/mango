package io.mango.notice.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.NoticeApi;
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
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeChannelReferenceImpactQuery;
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

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(
            @SpringQueryMap NoticeBusinessTypePageQuery query);

    @Override
    @GetMapping("/site/business-types")
    R<PageResult<NoticeBusinessTypeVO>> listEnabledBusinessTypes(
            @SpringQueryMap NoticeBusinessTypePageQuery query);

    @Override
    @PostMapping("/business-types")
    R<NoticeBusinessTypeVO> createBusinessType(
            @RequestBody CreateNoticeBusinessTypeCommand command);

    @Override
    @PutMapping("/business-types")
    R<NoticeBusinessTypeVO> updateBusinessType(
            @RequestParam("id") Long id, @RequestBody UpdateNoticeBusinessTypeCommand command);

    @Override
    @DeleteMapping("/business-types")
    R<Boolean> deleteBusinessType(@RequestParam("id") Long id);

    @Override
    @PostMapping("/business-types/enable")
    R<Boolean> enableBusinessType(@RequestParam("id") Long id);

    @Override
    @PostMapping("/business-types/disable")
    R<Boolean> disableBusinessType(@RequestParam("id") Long id);

    @Override
    @GetMapping("/business-types/config-versions")
    R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(
            @RequestParam("businessTypeId") Long businessTypeId);

    @Override
    @PutMapping("/business-types/config-draft")
    R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(
            @RequestParam("businessTypeId") Long businessTypeId,
            @RequestBody SaveNoticeBusinessConfigCommand command);

    @Override
    @PostMapping("/business-types/config-draft/publish")
    R<Boolean> publishBusinessConfigDraft(@RequestParam("businessTypeId") Long businessTypeId);

    @Override
    @PostMapping("/business-types/config-versions/activate")
    R<Boolean> activateBusinessConfigVersion(
            @RequestParam("businessTypeId") Long businessTypeId,
            @RequestParam("version") Integer version);

    @Override
    @GetMapping("/business-types/channel-templates")
    R<List<NoticeChannelTemplateVO>> listChannelTemplates(
            @RequestParam("businessTypeId") Long businessTypeId);

    @Override
    @PutMapping("/business-types/channel-templates")
    R<NoticeChannelTemplateVO> saveChannelTemplate(
            @RequestParam("businessTypeId") Long businessTypeId,
            @RequestBody SaveNoticeChannelTemplateCommand command);

    @Override
    @PostMapping("/business-types/channel-templates/publish")
    R<Boolean> publishChannelTemplate(
            @RequestParam("businessTypeId") Long businessTypeId,
            @RequestParam("channelType") NoticeChannelType channelType);

    @Override
    @GetMapping("/channels")
    R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(
            @SpringQueryMap NoticeChannelConfigPageQuery query);

    @Override
    @PostMapping("/channels")
    R<NoticeChannelConfigVO> saveChannelConfig(@RequestBody SaveNoticeChannelConfigCommand command);

    @Override
    @GetMapping("/channel-route-tags")
    R<List<NoticeRouteTagVO>> listRouteTags(@SpringQueryMap NoticeRouteTagQuery query);

    @Override
    @PostMapping("/channel-route-tags")
    R<NoticeRouteTagVO> saveRouteTag(@RequestBody SaveNoticeRouteTagCommand command);

    @Override
    @DeleteMapping("/channel-route-tags")
    R<Boolean> deleteRouteTag(@RequestParam("id") Long id);

    @Override
    @GetMapping("/channels/reference-impact")
    R<NoticeChannelReferenceImpactVO> getChannelReferenceImpact(
            @SpringQueryMap NoticeChannelReferenceImpactQuery query);

    @Override
    @GetMapping("/internal/wecom-login-config")
    R<NoticeWecomLoginConfigVO> getWecomLoginConfig(
            @RequestParam(value = "channelConfigId", required = false) Long channelConfigId);

    @Override
    @DeleteMapping("/channels")
    R<Boolean> deleteChannelConfig(@RequestParam("id") Long id);

    @Override
    @GetMapping("/tasks")
    R<PageResult<NoticeTaskVO>> listTasks(@SpringQueryMap NoticeTaskPageQuery query);

    @Override
    @GetMapping("/records")
    R<PageResult<NoticeSendRecordVO>> listSendRecords(
            @SpringQueryMap NoticeSendRecordPageQuery query);

    @Override
    @PostMapping("/records/retry")
    R<Boolean> retrySendRecord(@RequestParam("id") Long id);

    @Override
    @PostMapping("/records/retry-batch")
    R<Boolean> retrySendRecords(@RequestBody RetryNoticeSendRecordsCommand command);

    @Override
    @PostMapping("/records/manual-success")
    R<Boolean> markSendRecordManualSuccess(
            @RequestParam("id") Long id, @RequestBody HandleNoticeSendRecordCommand command);

    @Override
    @PostMapping("/records/manual-success-batch")
    R<Boolean> markSendRecordsManualSuccess(@RequestBody HandleNoticeSendRecordsCommand command);

    @Override
    @PostMapping("/records/ignore")
    R<Boolean> ignoreSendRecord(
            @RequestParam("id") Long id, @RequestBody HandleNoticeSendRecordCommand command);

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
    R<List<NoticeRecipientAccountVO>> listRecipientAccounts(
            @SpringQueryMap NoticeRecipientAccountQuery query);

    @Override
    @PostMapping("/recipient-accounts")
    R<NoticeRecipientAccountVO> saveRecipientAccount(
            @RequestBody SaveNoticeRecipientAccountCommand command);

    @Override
    @PostMapping("/wecom/users/sync")
    R<WecomUserSyncResultVO> syncWecomUsers(@RequestBody SyncWecomUsersCommand command);

    @Override
    @PostMapping("/recipient-accounts/disable")
    R<Boolean> disableRecipientAccount(
            @RequestParam("id") Long id,
            @RequestParam(value = "userId", required = false) Long userId);

    @Override
    @PostMapping("/recipient-accounts/default")
    R<Boolean> setDefaultRecipientAccount(
            @RequestParam("id") Long id,
            @RequestParam(value = "userId", required = false) Long userId);

    @Override
    @GetMapping("/receive-preferences")
    R<List<NoticeReceivePreferenceVO>> listReceivePreferences(
            @SpringQueryMap NoticeReceivePreferenceQuery query);

    @Override
    @PutMapping("/receive-preferences")
    R<NoticeReceivePreferenceVO> saveReceivePreference(
            @RequestBody SaveNoticeReceivePreferenceCommand command);

    @Override
    @GetMapping("/site/my/messages")
    R<PageResult<NoticeSiteMessageVO>> listSiteMessages(
            @SpringQueryMap NoticeSiteMessagePageQuery query);

    @Override
    @GetMapping("/site/my/messages/detail")
    R<NoticeSiteMessageVO> getSiteMessage(@RequestParam("id") Long id);

    @Override
    @PostMapping("/site/my/messages/actions")
    R<NoticeSiteMessageActionRequestVO> executeSiteMessageAction(
            @RequestBody ExecuteNoticeSiteMessageActionCommand command);

    @Override
    @PostMapping("/internal/site/actions/complete")
    R<NoticeSiteMessageActionRequestVO> completeSiteMessageAction(
            @RequestBody CompleteNoticeSiteMessageActionCommand command);

    @Override
    @GetMapping("/site/my/unread-count")
    R<NoticeUnreadCountVO> unreadCount();

    @Override
    @GetMapping("/site/my/unread-category-stats")
    R<NoticeUnreadCategoryStatsVO> unreadCategoryStats();

    @Override
    @PostMapping("/site/my/messages/read")
    R<Boolean> markSiteMessageRead(@RequestParam("id") Long id);

    @Override
    @PostMapping("/site/my/messages/read-batch")
    R<Boolean> markSiteMessagesRead(@RequestBody MarkNoticeReadCommand command);

    @Override
    @PostMapping("/site/my/messages/read-all")
    R<Boolean> markAllSiteMessagesRead();

    @Override
    @PostMapping("/site/my/messages/delete")
    R<Boolean> deleteSiteMessage(@RequestParam("id") Long id);
}
