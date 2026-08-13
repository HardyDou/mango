package io.mango.notice.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
import io.mango.notice.core.service.INoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "业务通知配置、多渠道发送、系统消息与发送记录接口")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring singleton service is injected and intentionally shared")
public class NoticeController implements NoticeApi {
    private final INoticeService noticeService;

    @Override
    @PostMapping("/send")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:task:create")
    @Operation(summary = "按业务类型发送通知", description = "按业务类型已启用渠道模板生成通知任务和发送记录")
    public R<NoticeSendResultVO> send(@RequestBody SendNoticeCommand command) {
        return R.ok(noticeService.send(command));
    }

    @Override
    @PostMapping("/site/messages")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:create")
    @Operation(summary = "发送系统消息", description = "管理端快捷发送系统消息，仍走统一任务和发送记录")
    public R<NoticeSendResultVO> sendSiteMessage(@RequestBody SendNoticeCommand command) {
        command.setChannelTypes(List.of(NoticeChannelType.SITE));
        return R.ok(noticeService.send(command));
    }

    @Override
    @GetMapping("/business-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
    @Operation(summary = "查询业务通知配置", description = "分页查询业务类型和参数 schema")
    public R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(
            @ParameterObject NoticeBusinessTypePageQuery query) {
        return R.ok(noticeService.listBusinessTypes(query));
    }

    @Override
    @GetMapping("/site/business-types")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询个人通知可用消息类型")
    @Operation(summary = "查询个人通知可用消息类型", description = "登录接口。只返回当前租户已启用的业务消息类型")
    public R<PageResult<NoticeBusinessTypeVO>> listEnabledBusinessTypes(
            @ParameterObject NoticeBusinessTypePageQuery query) {
        return R.ok(noticeService.listEnabledBusinessTypes(query));
    }

    @Override
    @PostMapping("/business-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:create")
    @Operation(summary = "创建业务通知配置", description = "创建业务类型和参数 schema")
    public R<NoticeBusinessTypeVO> createBusinessType(
            @RequestBody CreateNoticeBusinessTypeCommand command) {
        return R.ok(noticeService.createBusinessType(command));
    }

    @Override
    @PutMapping("/business-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
    @Operation(summary = "更新业务通知基础信息", description = "只更新业务类型名称、分组和描述；运行时发布配置通过草稿和发布接口维护")
    public R<NoticeBusinessTypeVO> updateBusinessType(
            @Parameter(description = "业务消息配置ID", required = true) @RequestParam("id") Long id,
            @RequestBody UpdateNoticeBusinessTypeCommand command) {
        return R.ok(noticeService.updateBusinessType(id, command));
    }

    @Override
    @DeleteMapping("/business-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:delete")
    @Operation(summary = "删除业务消息配置", description = "删除业务消息定义、配置版本和渠道模板；存在待发送或发送中任务时不允许删除")
    public R<Boolean> deleteBusinessType(
            @Parameter(description = "业务消息配置ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.deleteBusinessType(id));
    }

    @Override
    @PostMapping("/business-types/enable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:enable")
    @Operation(summary = "启用业务通知配置", description = "启用业务类型")
    public R<Boolean> enableBusinessType(
            @Parameter(description = "业务消息配置ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.enableBusinessType(id));
    }

    @Override
    @PostMapping("/business-types/disable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:enable")
    @Operation(summary = "停用业务通知配置", description = "停用业务类型")
    public R<Boolean> disableBusinessType(
            @Parameter(description = "业务消息配置ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.disableBusinessType(id));
    }

    @Override
    @GetMapping("/business-types/config-versions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
    @Operation(summary = "查询业务发布配置版本", description = "查询业务类型参数 schema、默认优先级和幂等策略的草稿、生效与历史版本")
    public R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId) {
        return R.ok(noticeService.listBusinessConfigVersions(businessTypeId));
    }

    @Override
    @PutMapping("/business-types/config-draft")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
    @Operation(summary = "保存业务发布配置草稿", description = "保存业务类型参数 schema、默认优先级和幂等策略草稿，保存后不立即生效")
    public R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId,
            @RequestBody SaveNoticeBusinessConfigCommand command) {
        return R.ok(noticeService.saveBusinessConfigDraft(businessTypeId, command));
    }

    @Override
    @PostMapping("/business-types/config-draft/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
    @Operation(summary = "发布业务发布配置草稿", description = "发布业务类型运行时配置，旧生效版本转历史，新草稿成为当前生效版本")
    public R<Boolean> publishBusinessConfigDraft(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId) {
        return R.ok(noticeService.publishBusinessConfigDraft(businessTypeId));
    }

    @Override
    @PostMapping("/business-types/config-versions/activate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
    @Operation(summary = "启用业务配置历史版本", description = "复制指定历史版本生成新版本并发布，保留版本审计链路")
    public R<Boolean> activateBusinessConfigVersion(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId,
            @Parameter(description = "配置版本", required = true) @RequestParam("version")
                    Integer version) {
        return R.ok(noticeService.activateBusinessConfigVersion(businessTypeId, version));
    }

    @Override
    @GetMapping("/business-types/channel-templates")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
    @Operation(summary = "查询业务渠道模板", description = "查询业务类型下各渠道模板")
    public R<List<NoticeChannelTemplateVO>> listChannelTemplates(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId) {
        return R.ok(noticeService.listChannelTemplates(businessTypeId));
    }

    @Override
    @PutMapping("/business-types/channel-templates")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
    @Operation(summary = "保存业务渠道模板", description = "保存指定业务类型下的渠道模板草稿")
    public R<NoticeChannelTemplateVO> saveChannelTemplate(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId,
            @RequestBody SaveNoticeChannelTemplateCommand command) {
        return R.ok(noticeService.saveChannelTemplate(businessTypeId, command));
    }

    @Override
    @PostMapping("/business-types/channel-templates/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
    @Operation(summary = "发布业务渠道模板", description = "发布草稿模板为当前生效版本")
    public R<Boolean> publishChannelTemplate(
            @Parameter(description = "业务类型ID", required = true) @RequestParam("businessTypeId")
                    Long businessTypeId,
            @Parameter(description = "渠道类型", required = true) @RequestParam("channelType")
                    NoticeChannelType channelType) {
        return R.ok(noticeService.publishChannelTemplate(businessTypeId, channelType));
    }

    @Override
    @GetMapping("/channels")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:view")
    @Operation(summary = "查询渠道配置", description = "分页查询通知渠道配置")
    public R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(
            @ParameterObject NoticeChannelConfigPageQuery query) {
        return R.ok(noticeService.listChannelConfigs(query));
    }

    @Override
    @PostMapping("/channels")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:create")
    @Operation(summary = "保存渠道配置", description = "保存短信、邮件、微信、企微、钉钉等渠道配置")
    public R<NoticeChannelConfigVO> saveChannelConfig(
            @RequestBody SaveNoticeChannelConfigCommand command) {
        return R.ok(noticeService.saveChannelConfig(command));
    }

    @Override
    @GetMapping("/channel-route-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:view")
    @Operation(summary = "查询渠道路由标签", description = "按渠道类型和关键词查询路由标签及候选账号摘要")
    public R<List<NoticeRouteTagVO>> listRouteTags(@ParameterObject NoticeRouteTagQuery query) {
        return R.ok(noticeService.listRouteTags(query));
    }

    @Override
    @PostMapping("/channel-route-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:edit")
    @Operation(summary = "保存渠道路由标签", description = "创建或更新渠道路由标签")
    public R<NoticeRouteTagVO> saveRouteTag(@RequestBody SaveNoticeRouteTagCommand command) {
        return R.ok(noticeService.saveRouteTag(command));
    }

    @Override
    @DeleteMapping("/channel-route-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:delete")
    @Operation(summary = "删除未被引用的渠道路由标签", description = "删除未被消息模板引用的渠道路由标签")
    public R<Boolean> deleteRouteTag(
            @Parameter(description = "路由标签 ID") @RequestParam("id") Long id) {
        return R.ok(noticeService.deleteRouteTag(id));
    }

    @Override
    @GetMapping("/channels/reference-impact")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:view")
    @Operation(summary = "查询渠道或标签的模板引用影响", description = "查询渠道账号或路由标签关联的业务模板摘要")
    public R<NoticeChannelReferenceImpactVO> getChannelReferenceImpact(
            @ParameterObject NoticeChannelReferenceImpactQuery query) {
        return R.ok(noticeService.getChannelReferenceImpact(query));
    }

    @Override
    @GetMapping("/internal/wecom-login-config")
    @ApiAccess(mode = ApiResourceAccessMode.INTERNAL, desc = "内部读取企业微信扫码登录配置")
    @Operation(summary = "内部读取企业微信扫码登录配置", description = "内部接口。供认证服务读取企业微信渠道配置，Secret 不对前端开放")
    public R<NoticeWecomLoginConfigVO> getWecomLoginConfig(
            @Parameter(description = "渠道配置ID")
                    @RequestParam(value = "channelConfigId", required = false)
                    Long channelConfigId) {
        return R.ok(noticeService.getWecomLoginConfig(channelConfigId));
    }

    @Override
    @DeleteMapping("/channels")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:delete")
    @Operation(summary = "删除渠道配置", description = "删除未被消息模板引用且非系统内置的渠道配置")
    public R<Boolean> deleteChannelConfig(
            @Parameter(description = "渠道配置ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.deleteChannelConfig(id));
    }

    @Override
    @GetMapping("/tasks")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:task:view")
    @Operation(summary = "查询通知任务", description = "分页查询通知任务")
    public R<PageResult<NoticeTaskVO>> listTasks(@ParameterObject NoticeTaskPageQuery query) {
        return R.ok(noticeService.listTasks(query));
    }

    @Override
    @GetMapping("/records")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:record:view")
    @Operation(summary = "查询发送记录", description = "分页查询每个接收人每个渠道的发送记录")
    public R<PageResult<NoticeSendRecordVO>> listSendRecords(
            @ParameterObject NoticeSendRecordPageQuery query) {
        return R.ok(noticeService.listSendRecords(query));
    }

    @Override
    @GetMapping("/inbound-messages")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:inbound:view")
    @Operation(summary = "查询接收消息", description = "管理端分页查询邮件和企业微信入站消息")
    public R<PageResult<NoticeInboundMessageVO>> listInboundMessages(
            @ParameterObject NoticeInboundMessagePageQuery query) {
        return R.ok(noticeService.listInboundMessages(query));
    }

    @Override
    @GetMapping("/inbound-messages/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:inbound:view")
    @Operation(summary = "查询接收消息详情", description = "管理端查询入站消息正文、广播状态和附件文件引用")
    public R<NoticeInboundMessageVO> getInboundMessage(
            @Parameter(description = "入站消息ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.getInboundMessage(id));
    }

    @Override
    @PostMapping("/records/retry")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "重试发送记录", description = "对失败、等待重试或最终失败的发送记录立即重试")
    public R<Boolean> retrySendRecord(
            @Parameter(description = "发送记录ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.retrySendRecord(id));
    }

    @Override
    @PostMapping("/records/retry-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "批量重试发送记录", description = "批量对失败、等待重试或最终失败的发送记录立即重试")
    public R<Boolean> retrySendRecords(@RequestBody RetryNoticeSendRecordsCommand command) {
        return R.ok(noticeService.retrySendRecords(command));
    }

    @Override
    @PostMapping("/records/manual-success")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "标记发送记录人工成功", description = "用于外部实际已成功但系统未收到成功回执的失败记录")
    public R<Boolean> markSendRecordManualSuccess(
            @Parameter(description = "发送记录ID", required = true) @RequestParam("id") Long id,
            @RequestBody HandleNoticeSendRecordCommand command) {
        return R.ok(noticeService.markSendRecordManualSuccess(id, command));
    }

    @Override
    @PostMapping("/records/manual-success-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "批量标记发送记录人工成功", description = "批量用于外部实际已成功但系统未收到成功回执的失败记录")
    public R<Boolean> markSendRecordsManualSuccess(
            @RequestBody HandleNoticeSendRecordsCommand command) {
        return R.ok(noticeService.markSendRecordsManualSuccess(command));
    }

    @Override
    @PostMapping("/records/ignore")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "忽略发送失败", description = "将失败记录标记为已忽略，不再进入失败重试池")
    public R<Boolean> ignoreSendRecord(
            @Parameter(description = "发送记录ID", required = true) @RequestParam("id") Long id,
            @RequestBody HandleNoticeSendRecordCommand command) {
        return R.ok(noticeService.ignoreSendRecord(id, command));
    }

    @Override
    @PostMapping("/records/ignore-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:retry:edit")
    @Operation(summary = "批量忽略发送失败", description = "批量将失败记录标记为已忽略，不再进入失败重试池")
    public R<Boolean> ignoreSendRecords(@RequestBody HandleNoticeSendRecordsCommand command) {
        return R.ok(noticeService.ignoreSendRecords(command));
    }

    @Override
    @GetMapping("/settings")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:setting:view")
    @Operation(summary = "获取通知设置", description = "获取当前机构通知设置，未保存时返回默认设置")
    public R<NoticeSettingsVO> getSettings() {
        return R.ok(noticeService.getSettings());
    }

    @Override
    @PutMapping("/settings")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:setting:edit")
    @Operation(summary = "保存通知设置", description = "保存当前机构通知提示、重试和保留周期设置")
    public R<Boolean> saveSettings(@RequestBody SaveNoticeSettingsCommand command) {
        return R.ok(noticeService.saveSettings(command));
    }

    @Override
    @GetMapping("/recipient-accounts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:receive-setting:view")
    @Operation(summary = "查询通知接收账户", description = "权限接口。查询当前用户或指定用户的通知接收账户")
    public R<List<NoticeRecipientAccountVO>> listRecipientAccounts(
            @ParameterObject NoticeRecipientAccountQuery query) {
        return R.ok(noticeService.listRecipientAccounts(query));
    }

    @Override
    @PostMapping("/recipient-accounts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:receive-setting:edit")
    @Operation(summary = "保存通知接收账户", description = "权限接口。新增或更新手机号、邮箱等通知接收账户")
    public R<NoticeRecipientAccountVO> saveRecipientAccount(
            @RequestBody SaveNoticeRecipientAccountCommand command) {
        return R.ok(noticeService.saveRecipientAccount(command));
    }

    @Override
    @PostMapping("/wecom/users/sync")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:add")
    @Operation(summary = "同步企业微信用户", description = "从企业微信通讯录同步成员，并绑定企业微信通知接收账户")
    public R<WecomUserSyncResultVO> syncWecomUsers(@RequestBody SyncWecomUsersCommand command) {
        return R.ok(noticeService.syncWecomUsers(command));
    }

    @Override
    @PostMapping("/recipient-accounts/disable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:receive-setting:edit")
    @Operation(summary = "禁用通知接收账户", description = "权限接口。禁用当前用户或指定用户的通知接收账户")
    public R<Boolean> disableRecipientAccount(
            @Parameter(description = "接收账户ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "目标用户ID") @RequestParam(value = "userId", required = false)
                    Long userId) {
        return R.ok(noticeService.disableRecipientAccount(id, userId));
    }

    @Override
    @PostMapping("/recipient-accounts/default")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:receive-setting:edit")
    @Operation(summary = "设置默认通知接收账户", description = "权限接口。设置同类型默认通知接收账户")
    public R<Boolean> setDefaultRecipientAccount(
            @Parameter(description = "接收账户ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "目标用户ID") @RequestParam(value = "userId", required = false)
                    Long userId) {
        return R.ok(noticeService.setDefaultRecipientAccount(id, userId));
    }

    @Override
    @GetMapping("/receive-preferences")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户通知接收偏好")
    @Operation(summary = "查询通知接收偏好", description = "登录接口。查询当前用户按全局、业务域或单消息维度配置的接收偏好")
    public R<List<NoticeReceivePreferenceVO>> listReceivePreferences(
            @ParameterObject NoticeReceivePreferenceQuery query) {
        return R.ok(noticeService.listReceivePreferences(query));
    }

    @Override
    @PutMapping("/receive-preferences")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "保存当前用户通知接收偏好")
    @Operation(summary = "保存通知接收偏好", description = "登录接口。保存当前用户对业务域、单消息和渠道的接收偏好")
    public R<NoticeReceivePreferenceVO> saveReceivePreference(
            @RequestBody SaveNoticeReceivePreferenceCommand command) {
        return R.ok(noticeService.saveReceivePreference(command));
    }

    @Override
    @GetMapping("/site/my/messages")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询当前用户站内信")
    @Operation(summary = "分页查询我的系统消息", description = "登录接口。分页查询当前用户站内信")
    public R<PageResult<NoticeSiteMessageVO>> listSiteMessages(
            @ParameterObject NoticeSiteMessagePageQuery query) {
        return R.ok(noticeService.listSiteMessages(query));
    }

    @Override
    @GetMapping("/site/my/messages/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户站内信详情")
    @Operation(summary = "获取我的系统消息详情", description = "登录接口。查询当前用户可见的站内信详情")
    public R<NoticeSiteMessageVO> getSiteMessage(
            @Parameter(description = "系统消息ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.getSiteMessage(id));
    }

    @Override
    @PostMapping("/site/my/messages/actions")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "执行当前用户站内信动作")
    @Operation(summary = "执行我的系统消息动作", description = "登录接口。提交当前用户可见站内信的受控动作入口")
    public R<NoticeSiteMessageActionRequestVO> executeSiteMessageAction(
            @RequestBody ExecuteNoticeSiteMessageActionCommand command) {
        return R.ok(noticeService.executeSiteMessageAction(command));
    }

    @Override
    @PostMapping("/internal/site/actions/complete")
    @ApiAccess(mode = ApiResourceAccessMode.INTERNAL, desc = "内部完成系统消息动作")
    @Operation(summary = "内部完成系统消息动作", description = "内部接口。业务模块完成动作处理后回写请求和动作状态")
    public R<NoticeSiteMessageActionRequestVO> completeSiteMessageAction(
            @RequestBody CompleteNoticeSiteMessageActionCommand command) {
        return R.ok(noticeService.completeSiteMessageAction(command));
    }

    @Override
    @GetMapping("/site/my/unread-count")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户站内信未读数")
    @Operation(summary = "获取我的系统消息未读数", description = "登录接口。查询当前用户站内信未读数量")
    public R<NoticeUnreadCountVO> unreadCount() {
        return R.ok(noticeService.unreadCount());
    }

    @Override
    @GetMapping("/site/my/unread-category-stats")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户站内信分类统计")
    @Operation(summary = "获取我的系统消息未读分类统计", description = "登录接口。按审批、系统和业务分类统计当前用户未读站内信")
    public R<NoticeUnreadCategoryStatsVO> unreadCategoryStats() {
        return R.ok(noticeService.unreadCategoryStats());
    }

    @Override
    @PostMapping("/site/my/messages/read")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "标记当前用户站内信已读")
    @Operation(summary = "标记我的系统消息已读", description = "登录接口。标记当前用户的一条站内信为已读")
    public R<Boolean> markSiteMessageRead(
            @Parameter(description = "系统消息ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.markSiteMessageRead(id));
    }

    @Override
    @PostMapping("/site/my/messages/read-batch")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "批量标记当前用户站内信已读")
    @Operation(summary = "批量标记我的系统消息已读", description = "登录接口。批量标记当前用户站内信为已读")
    public R<Boolean> markSiteMessagesRead(@RequestBody MarkNoticeReadCommand command) {
        return R.ok(noticeService.markSiteMessagesRead(command));
    }

    @Override
    @PostMapping("/site/my/messages/read-all")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "全部标记当前用户站内信已读")
    @Operation(summary = "全部标记已读", description = "登录接口。标记当前用户全部未读站内信为已读")
    public R<Boolean> markAllSiteMessagesRead() {
        return R.ok(noticeService.markAllSiteMessagesRead());
    }

    @Override
    @PostMapping("/site/my/messages/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除当前用户站内信")
    @Operation(summary = "删除我的系统消息", description = "登录接口。删除当前用户的一条站内信")
    public R<Boolean> deleteSiteMessage(
            @Parameter(description = "系统消息ID", required = true) @RequestParam("id") Long id) {
        return R.ok(noticeService.deleteSiteMessage(id));
    }
}
