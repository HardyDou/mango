package io.mango.notice.core.service;

import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;

import java.util.List;

/** Owns business definition, version, template and channel configuration transactions. */
public interface INoticeConfigurationService {

    PageResult<NoticeBusinessTypeVO> listBusinessTypes(NoticeBusinessTypePageQuery query);

    NoticeBusinessTypeVO createBusinessType(CreateNoticeBusinessTypeCommand command);

    NoticeBusinessTypeVO updateBusinessType(Long id, UpdateNoticeBusinessTypeCommand command);

    boolean deleteBusinessType(Long id);

    boolean enableBusinessType(Long id);

    boolean disableBusinessType(Long id);

    List<NoticeBusinessConfigVersionVO> listBusinessConfigVersions(Long businessTypeId);

    NoticeBusinessConfigVersionVO saveBusinessConfigDraft(Long businessTypeId,
                                                           SaveNoticeBusinessConfigCommand command);

    boolean publishBusinessConfigDraft(Long businessTypeId);

    boolean activateBusinessConfigVersion(Long businessTypeId, Integer version);

    List<NoticeChannelTemplateVO> listChannelTemplates(Long businessTypeId);

    NoticeChannelTemplateVO saveChannelTemplate(Long businessTypeId, SaveNoticeChannelTemplateCommand command);

    boolean publishChannelTemplate(Long businessTypeId, NoticeChannelType channelType);

    PageResult<NoticeChannelConfigVO> listChannelConfigs(NoticeChannelConfigPageQuery query);

    NoticeChannelConfigVO saveChannelConfig(SaveNoticeChannelConfigCommand command);

    NoticeWecomLoginConfigVO getWecomLoginConfig(Long channelConfigId);

    boolean deleteChannelConfig(Long id);
}
