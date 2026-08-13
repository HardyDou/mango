package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.query.NoticeInboundMessagePageQuery;
import io.mango.notice.api.vo.NoticeInboundAttachmentVO;
import io.mango.notice.api.vo.NoticeInboundMessageVO;
import io.mango.notice.core.entity.NoticeInboundAttachmentEntity;
import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import io.mango.notice.core.mapper.NoticeInboundAttachmentMapper;
import io.mango.notice.core.mapper.NoticeInboundMessageMapper;
import io.mango.notice.core.service.INoticeInboundQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeInboundQueryService implements INoticeInboundQueryService {

    private final NoticeInboundMessageMapper messageMapper;
    private final NoticeInboundAttachmentMapper attachmentMapper;

    @Override
    public PageResult<NoticeInboundMessageVO> listInboundMessages(NoticeInboundMessagePageQuery query) {
        Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息查询条件不能为空");
        LambdaQueryWrapper<NoticeInboundMessageEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getChannelType() != null) {
            wrapper.eq(NoticeInboundMessageEntity::getChannelType, query.getChannelType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(NoticeInboundMessageEntity::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(NoticeInboundMessageEntity::getSubject, keyword)
                    .or().like(NoticeInboundMessageEntity::getFromAddress, keyword)
                    .or().like(NoticeInboundMessageEntity::getMessageId, keyword));
        }
        if (query.getStartTime() != null) {
            wrapper.ge(NoticeInboundMessageEntity::getReceivedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(NoticeInboundMessageEntity::getReceivedAt, query.getEndTime());
        }
        wrapper.orderByDesc(NoticeInboundMessageEntity::getReceivedAt);
        Page<NoticeInboundMessageEntity> result = messageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toSummary).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public NoticeInboundMessageVO getInboundMessage(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息ID不能为空");
        NoticeInboundMessageEntity entity = messageMapper.selectById(id);
        Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息不存在");
        NoticeInboundMessageVO result = toSummary(entity);
        result.setBodyText(entity.getBodyText());
        result.setBodyHtml(entity.getBodyHtml());
        List<NoticeInboundAttachmentEntity> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<NoticeInboundAttachmentEntity>()
                        .eq(NoticeInboundAttachmentEntity::getMessageId, id)
                        .orderByAsc(NoticeInboundAttachmentEntity::getAttachmentIndex));
        result.setAttachments(attachments.stream().map(this::toAttachment).toList());
        return result;
    }

    private NoticeInboundMessageVO toSummary(NoticeInboundMessageEntity entity) {
        NoticeInboundMessageVO result = new NoticeInboundMessageVO();
        result.setId(entity.getId());
        result.setChannelConfigId(entity.getChannelConfigId());
        result.setChannelType(entity.getChannelType());
        result.setProviderCode(entity.getProviderCode());
        result.setMessageId(entity.getMessageId());
        result.setSubject(entity.getSubject());
        result.setFromAddress(entity.getFromAddress());
        result.setToAddressesJson(entity.getToAddressesJson());
        result.setStatus(entity.getStatus());
        result.setEventId(entity.getEventId());
        result.setFailureCode(entity.getFailureCode());
        result.setFailureReason(entity.getFailureReason());
        result.setAttemptCount(entity.getAttemptCount());
        result.setReceivedAt(entity.getReceivedAt());
        result.setProcessedAt(entity.getProcessedAt());
        return result;
    }

    private NoticeInboundAttachmentVO toAttachment(NoticeInboundAttachmentEntity entity) {
        NoticeInboundAttachmentVO result = new NoticeInboundAttachmentVO();
        result.setId(entity.getId());
        result.setFileId(entity.getFileId());
        result.setFileName(entity.getFileName());
        result.setContentType(entity.getContentType());
        result.setFileSize(entity.getFileSize());
        result.setStatus(entity.getStatus());
        result.setFailureReason(entity.getFailureReason());
        return result;
    }
}
