package io.mango.notice.core.controller;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.core.service.INoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "业务通知配置、多渠道发送、站内信与发送记录接口")
public class NoticeController implements NoticeApi {

 private final INoticeService noticeService;
 private final ISecurityContextProvider securityContextProvider;

 @Override
 @PostMapping("/send")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:task:create")
 @Operation(summary = "按业务类型发送通知", description = "按业务类型已启用渠道模板生成通知任务和发送记录")
 public R<NoticeSendResultVO> send(@RequestBody @Valid SendNoticeCommand command) {
 return R.ok(noticeService.send(command));
 }

 @Override
 @PostMapping("/site/messages")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:create")
 @Operation(summary = "发送站内信", description = "管理端快捷发送站内信，仍走统一任务和发送记录")
 public R<NoticeSendResultVO> sendSiteMessage(@RequestBody @Valid SendNoticeCommand command) {
 command.setChannelTypes(List.of(NoticeChannelType.SITE));
 return R.ok(noticeService.send(command));
 }

 @Override
 @GetMapping("/business-types")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
 @Operation(summary = "查询业务通知配置", description = "分页查询业务类型和参数 schema")
 public R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(@ParameterObject NoticeBusinessTypePageQuery query) {
 return R.ok(noticeService.listBusinessTypes(query));
 }

 @Override
 @PostMapping("/business-types")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:create")
 @Operation(summary = "创建业务通知配置", description = "创建业务类型和参数 schema")
 public R<NoticeBusinessTypeVO> createBusinessType(@RequestBody @Valid CreateNoticeBusinessTypeCommand command) {
 return R.ok(noticeService.createBusinessType(command));
 }

 @Override
 @PutMapping("/business-types/{id}")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
 @Operation(summary = "更新业务通知基础信息", description = "只更新业务类型名称、分组和描述；运行时发布配置通过草稿和发布接口维护")
 public R<NoticeBusinessTypeVO> updateBusinessType(@PathVariable Long id,
 @RequestBody @Valid UpdateNoticeBusinessTypeCommand command) {
 return R.ok(noticeService.updateBusinessType(id, command));
 }

 @Override
 @PostMapping("/business-types/{id}/enable")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:enable")
 @Operation(summary = "启用业务通知配置", description = "启用业务类型")
 public R<Boolean> enableBusinessType(@PathVariable Long id) {
 return R.ok(noticeService.enableBusinessType(id));
 }

 @Override
 @PostMapping("/business-types/{id}/disable")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:enable")
 @Operation(summary = "停用业务通知配置", description = "停用业务类型")
 public R<Boolean> disableBusinessType(@PathVariable Long id) {
 return R.ok(noticeService.disableBusinessType(id));
 }

 @Override
 @GetMapping("/business-types/{businessTypeId}/config-versions")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
 @Operation(summary = "查询业务发布配置版本", description = "查询业务类型参数 schema、默认优先级和幂等策略的草稿、生效与历史版本")
 public R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(@PathVariable Long businessTypeId) {
 return R.ok(noticeService.listBusinessConfigVersions(businessTypeId));
 }

 @Override
 @PutMapping("/business-types/{businessTypeId}/config-draft")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
 @Operation(summary = "保存业务发布配置草稿", description = "保存业务类型参数 schema、默认优先级和幂等策略草稿，保存后不立即生效")
 public R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(@PathVariable Long businessTypeId,
 @RequestBody @Valid SaveNoticeBusinessConfigCommand command) {
 return R.ok(noticeService.saveBusinessConfigDraft(businessTypeId, command));
 }

 @Override
 @PostMapping("/business-types/{businessTypeId}/config-draft/publish")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
 @Operation(summary = "发布业务发布配置草稿", description = "发布业务类型运行时配置，旧生效版本转历史，新草稿成为当前生效版本")
 public R<Boolean> publishBusinessConfigDraft(@PathVariable Long businessTypeId) {
 return R.ok(noticeService.publishBusinessConfigDraft(businessTypeId));
 }

 @Override
 @PostMapping("/business-types/{businessTypeId}/config-versions/{version}/activate")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
 @Operation(summary = "启用业务配置历史版本", description = "复制指定历史版本生成新版本并发布，保留版本审计链路")
 public R<Boolean> activateBusinessConfigVersion(@PathVariable Long businessTypeId, @PathVariable Integer version) {
 return R.ok(noticeService.activateBusinessConfigVersion(businessTypeId, version));
 }

 @Override
 @GetMapping("/business-types/{businessTypeId}/channel-templates")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:view")
 @Operation(summary = "查询业务渠道模板", description = "查询业务类型下各渠道模板")
 public R<List<NoticeChannelTemplateVO>> listChannelTemplates(@PathVariable Long businessTypeId) {
 return R.ok(noticeService.listChannelTemplates(businessTypeId));
 }

 @Override
 @PutMapping("/business-types/{businessTypeId}/channel-templates/{channelType}")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:edit")
 @Operation(summary = "保存业务渠道模板", description = "保存指定业务类型下的渠道模板草稿")
 public R<NoticeChannelTemplateVO> saveChannelTemplate(@PathVariable Long businessTypeId,
 @PathVariable NoticeChannelType channelType, @RequestBody @Valid SaveNoticeChannelTemplateCommand command) {
 return R.ok(noticeService.saveChannelTemplate(businessTypeId, channelType, command));
 }

 @Override
 @PostMapping("/business-types/{businessTypeId}/channel-templates/{channelType}/publish")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:business:publish")
 @Operation(summary = "发布业务渠道模板", description = "发布草稿模板为当前生效版本")
 public R<Boolean> publishChannelTemplate(@PathVariable Long businessTypeId,
 @PathVariable NoticeChannelType channelType) {
 return R.ok(noticeService.publishChannelTemplate(businessTypeId, channelType));
 }

 @Override
 @GetMapping("/channels")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:view")
 @Operation(summary = "查询渠道配置", description = "分页查询通知渠道配置")
 public R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(@ParameterObject NoticeChannelConfigPageQuery query) {
 return R.ok(noticeService.listChannelConfigs(query));
 }

 @Override
 @PostMapping("/channels")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:channel:create")
 @Operation(summary = "保存渠道配置", description = "保存短信、邮件、微信、企微、钉钉等渠道配置")
 public R<NoticeChannelConfigVO> saveChannelConfig(@RequestBody @Valid SaveNoticeChannelConfigCommand command) {
 return R.ok(noticeService.saveChannelConfig(command));
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
 public R<PageResult<NoticeSendRecordVO>> listSendRecords(@ParameterObject NoticeSendRecordPageQuery query) {
 return R.ok(noticeService.listSendRecords(query));
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
 public R<Boolean> saveSettings(@RequestBody @Valid SaveNoticeSettingsCommand command) {
 return R.ok(noticeService.saveSettings(command));
 }

 @Override
 @GetMapping("/site/my/messages")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:view")
 @Operation(summary = "分页查询我的站内信", description = "分页查询当前用户站内信")
 public R<PageResult<NoticeSiteMessageVO>> listSiteMessages(@ParameterObject NoticeSiteMessagePageQuery query) {
 return R.ok(noticeService.listSiteMessages(currentUserId(), query));
 }

 @Override
 @GetMapping("/site/my/messages/{id}")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:view")
 @Operation(summary = "获取我的站内信详情", description = "查询当前用户可见的站内信详情")
 public R<NoticeSiteMessageVO> getSiteMessage(@Parameter(description = "站内信ID") @PathVariable Long id) {
 NoticeSiteMessageVO message = noticeService.getSiteMessage(id, currentUserId());
 return message == null ? R.fail(404, "站内信不存在") : R.ok(message);
 }

 @Override
 @GetMapping("/site/my/unread-count")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:view")
 @Operation(summary = "获取我的站内信未读数", description = "查询当前用户站内信未读数量")
 public R<NoticeUnreadCountVO> unreadCount() {
 return R.ok(noticeService.unreadCount(currentUserId()));
 }

 @Override
 @PostMapping("/site/my/messages/{id}/read")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:edit")
 @Operation(summary = "标记我的站内信已读", description = "标记当前用户的一条站内信为已读")
 public R<Boolean> markSiteMessageRead(@Parameter(description = "站内信ID") @PathVariable Long id) {
 return R.ok(noticeService.markSiteMessageRead(id, currentUserId()));
 }

 @Override
 @PostMapping("/site/my/messages/read-batch")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:edit")
 @Operation(summary = "批量标记我的站内信已读", description = "批量标记当前用户站内信为已读")
 public R<Boolean> markSiteMessagesRead(@RequestBody @Valid MarkNoticeReadCommand command) {
 return R.ok(noticeService.markSiteMessagesRead(command, currentUserId()));
 }

 @Override
 @PostMapping("/site/my/messages/read-all")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:edit")
 @Operation(summary = "全部标记已读", description = "标记当前用户全部未读站内信为已读")
 public R<Boolean> markAllSiteMessagesRead() {
 return R.ok(noticeService.markAllSiteMessagesRead(currentUserId()));
 }

 @Override
 @PostMapping("/site/my/messages/{id}/delete")
 @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:delete")
 @Operation(summary = "删除我的站内信", description = "删除当前用户的一条站内信")
 public R<Boolean> deleteSiteMessage(@Parameter(description = "站内信ID") @PathVariable Long id) {
 return R.ok(noticeService.deleteSiteMessage(id, currentUserId()));
 }

 private Long currentUserId() {
 return securityContextProvider.currentContext().userId();
 }
}
