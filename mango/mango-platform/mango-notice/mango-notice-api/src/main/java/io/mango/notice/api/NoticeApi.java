package io.mango.notice.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
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
import jakarta.validation.Valid;

import java.util.List;

public interface NoticeApi {

    R<NoticeSendResultVO> send(@Valid SendNoticeCommand command);

    R<NoticeSendResultVO> sendSiteMessage(@Valid SendNoticeCommand command);

    R<PageResult<NoticeBusinessTypeVO>> listBusinessTypes(NoticeBusinessTypePageQuery query);

    R<NoticeBusinessTypeVO> createBusinessType(@Valid CreateNoticeBusinessTypeCommand command);

    R<NoticeBusinessTypeVO> updateBusinessType(Long id, @Valid UpdateNoticeBusinessTypeCommand command);

    R<Boolean> deleteBusinessType(Long id);

    R<Boolean> enableBusinessType(Long id);

    R<Boolean> disableBusinessType(Long id);

    R<List<NoticeBusinessConfigVersionVO>> listBusinessConfigVersions(Long businessTypeId);

    R<NoticeBusinessConfigVersionVO> saveBusinessConfigDraft(Long businessTypeId,
            @Valid SaveNoticeBusinessConfigCommand command);

    R<Boolean> publishBusinessConfigDraft(Long businessTypeId);

    R<Boolean> activateBusinessConfigVersion(Long businessTypeId, Integer version);

    R<List<NoticeChannelTemplateVO>> listChannelTemplates(Long businessTypeId);

    R<NoticeChannelTemplateVO> saveChannelTemplate(Long businessTypeId, NoticeChannelType channelType,
            @Valid SaveNoticeChannelTemplateCommand command);

    R<Boolean> publishChannelTemplate(Long businessTypeId, NoticeChannelType channelType);

    R<PageResult<NoticeChannelConfigVO>> listChannelConfigs(NoticeChannelConfigPageQuery query);

    R<NoticeChannelConfigVO> saveChannelConfig(@Valid SaveNoticeChannelConfigCommand command);

    R<Boolean> deleteChannelConfig(Long id);

    R<PageResult<NoticeTaskVO>> listTasks(NoticeTaskPageQuery query);

    R<PageResult<NoticeSendRecordVO>> listSendRecords(NoticeSendRecordPageQuery query);

    R<NoticeSettingsVO> getSettings();

    R<Boolean> saveSettings(@Valid SaveNoticeSettingsCommand command);

    R<PageResult<NoticeSiteMessageVO>> listSiteMessages(NoticeSiteMessagePageQuery query);

    R<NoticeSiteMessageVO> getSiteMessage(Long id);

    R<NoticeUnreadCountVO> unreadCount();

    R<Boolean> markSiteMessageRead(Long id);

    R<Boolean> markSiteMessagesRead(@Valid MarkNoticeReadCommand command);

    R<Boolean> markAllSiteMessagesRead();

    R<Boolean> deleteSiteMessage(Long id);
}
