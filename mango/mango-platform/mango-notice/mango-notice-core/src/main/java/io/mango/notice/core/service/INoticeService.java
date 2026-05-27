package io.mango.notice.core.service;

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

import java.util.List;

public interface INoticeService {

 NoticeSendResultVO send(SendNoticeCommand command);

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

 boolean deleteChannelConfig(Long id);

 PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query);

 PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query);

 NoticeSettingsVO getSettings();

 boolean saveSettings(SaveNoticeSettingsCommand command);

 PageResult<NoticeSiteMessageVO> listSiteMessages(Long userId, NoticeSiteMessagePageQuery query);

 NoticeSiteMessageVO getSiteMessage(Long id, Long userId);

 NoticeUnreadCountVO unreadCount(Long userId);

 boolean markSiteMessageRead(Long id, Long userId);

 boolean markSiteMessagesRead(MarkNoticeReadCommand command, Long userId);

 boolean markAllSiteMessagesRead(Long userId);

 boolean deleteSiteMessage(Long id, Long userId);
}
