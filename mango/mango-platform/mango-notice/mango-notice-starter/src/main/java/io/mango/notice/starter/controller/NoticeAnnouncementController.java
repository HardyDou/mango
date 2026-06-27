package io.mango.notice.starter.controller;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.NoticeAnnouncementApi;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SaveNoticeAnnouncementCommand;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.query.NoticeAnnouncementIdQuery;
import io.mango.notice.api.query.NoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementStatsVO;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import io.mango.notice.core.service.INoticeAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Tag(name = "通知公告", description = "公告管理、发布、阅读和确认接口")
public class NoticeAnnouncementController implements NoticeAnnouncementApi {

    private final INoticeAnnouncementService announcementService;
    private final ISecurityContextProvider securityContextProvider;

    @Override
    @GetMapping("/announcements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:view")
    @Operation(summary = "分页查询公告", description = "管理端分页查询公告草稿、已发布和已下线记录")
    public R<PageResult<NoticeAnnouncementVO>> pageAnnouncements(@ParameterObject NoticeAnnouncementPageQuery query) {
        return R.ok(announcementService.pageAnnouncements(query));
    }

    @Override
    @GetMapping("/announcements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:view")
    @Operation(summary = "查询公告详情", description = "管理端查询公告详情、发布对象快照和统计")
    public R<NoticeAnnouncementVO> getAnnouncement(@ParameterObject @Valid NoticeAnnouncementIdQuery query) {
        return R.ok(announcementService.getAnnouncement(query.getId()));
    }

    @Override
    @PostMapping("/announcements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:create")
    @Operation(summary = "创建公告草稿", description = "创建公告草稿，可同时保存发布对象快照")
    public R<NoticeAnnouncementVO> createAnnouncement(@RequestBody @Valid SaveNoticeAnnouncementCommand command) {
        return R.ok(announcementService.createAnnouncement(command));
    }

    @Override
    @PutMapping("/announcements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:edit")
    @Operation(summary = "更新公告草稿", description = "只有草稿公告允许编辑")
    public R<NoticeAnnouncementVO> updateAnnouncement(@RequestBody @Valid SaveNoticeAnnouncementCommand command) {
        return R.ok(announcementService.updateAnnouncement(command.getId(), command));
    }

    @Override
    @PostMapping("/announcements/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:publish")
    @Operation(summary = "发布公告", description = "发布公告并按发布对象解析到用户级接收记录")
    public R<Boolean> publishAnnouncement(@RequestBody @Valid PublishNoticeAnnouncementCommand command) {
        return R.ok(announcementService.publishAnnouncement(command.getId(), command));
    }

    @Override
    @PostMapping("/announcements/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:offline")
    @Operation(summary = "下线公告", description = "下线已发布公告，用户端不再展示")
    public R<Boolean> offlineAnnouncement(@RequestBody @Valid NoticeAnnouncementIdQuery query) {
        return R.ok(announcementService.offlineAnnouncement(query.getId()));
    }

    @Override
    @GetMapping("/announcements/stats")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:announcement:view")
    @Operation(summary = "查询公告统计", description = "查询公告接收、已读和确认统计")
    public R<NoticeAnnouncementStatsVO> getAnnouncementStats(@ParameterObject @Valid NoticeAnnouncementIdQuery query) {
        return R.ok(announcementService.getAnnouncementStats(query.getId()));
    }

    @Override
    @GetMapping("/site/my/announcements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:view")
    @Operation(summary = "分页查询我的公告", description = "分页查询当前用户可见公告")
    public R<PageResult<NoticeAnnouncementVO>> pageMyAnnouncements(@ParameterObject MyNoticeAnnouncementPageQuery query) {
        return R.ok(announcementService.pageMyAnnouncements(currentUserId(), query));
    }

    @Override
    @GetMapping("/site/my/announcements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:view")
    @Operation(summary = "查询我的公告详情", description = "查询当前用户公告详情并标记已读")
    public R<NoticeAnnouncementVO> getMyAnnouncement(@ParameterObject @Valid NoticeAnnouncementIdQuery query) {
        return R.ok(announcementService.getMyAnnouncement(query.getId(), currentUserId()));
    }

    @Override
    @PostMapping("/site/my/announcements/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "notice:site:edit")
    @Operation(summary = "确认我的公告", description = "确认当前用户待确认公告")
    public R<Boolean> confirmMyAnnouncement(@RequestBody @Valid NoticeAnnouncementIdQuery query) {
        return R.ok(announcementService.confirmMyAnnouncement(query.getId(), currentUserId()));
    }

    private Long currentUserId() {
        return securityContextProvider.currentContext().userId();
    }
}
