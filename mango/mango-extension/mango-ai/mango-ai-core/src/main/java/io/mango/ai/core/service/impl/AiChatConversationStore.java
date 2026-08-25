package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiChatMessageVO;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.core.entity.AiChatConversationEntity;
import io.mango.ai.core.entity.AiChatMessageEntity;
import io.mango.ai.core.mapper.AiChatConversationMapper;
import io.mango.ai.core.mapper.AiChatMessageMapper;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.AiConversationExchange;
import io.mango.ai.core.service.AiConversationScope;
import io.mango.ai.core.service.IAiChatConversationStore;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** AI 对话与消息的唯一持久化访问入口。 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects mapper and ObjectMapper collaborators; copying container-managed services is not valid"))
public class AiChatConversationStore implements IAiChatConversationStore {
    private static final int MAX_CONVERSATIONS = 50;
    private static final int MAX_TITLE_CODE_POINTS = 40;
    private static final TypeReference<List<AiMessageContentPartVO>> CONTENT_PARTS_TYPE = new TypeReference<>() { };

    private final AiChatConversationMapper conversationMapper;
    private final AiChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiChatConversationVO> list(AiConversationScope scope) {
        return conversationMapper.selectList(new LambdaQueryWrapper<AiChatConversationEntity>()
                        .eq(AiChatConversationEntity::getTenantId, scope.tenantId())
                        .eq(AiChatConversationEntity::getUserId, scope.userId())
                        .eq(AiChatConversationEntity::getServiceCode, scope.serviceCode())
                        .orderByDesc(AiChatConversationEntity::getUpdatedAt)
                        .last("LIMIT " + MAX_CONVERSATIONS))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public AiChatConversationDetailVO detail(AiConversationScope scope) {
        AiChatConversationEntity conversation = requireConversation(scope);
        AiChatConversationDetailVO detail = new AiChatConversationDetailVO();
        copySummary(conversation, detail);
        detail.setMessages(messages(conversation.getId()).stream().map(this::toMessage).toList());
        return detail;
    }

    @Override
    public ConversationState load(AiConversationScope scope, int maxHistoryMessages) {
        AiChatConversationEntity conversation = findConversation(scope);
        if (conversation == null) {
            return new ConversationState(List.of());
        }
        List<AiChatMessageEntity> stored = messages(conversation.getId());
        int fromIndex = Math.max(0, stored.size() - maxHistoryMessages);
        List<ConversationMessage> history = stored.subList(fromIndex, stored.size()).stream()
                .map(message -> new ConversationMessage(message.getRole(), readParts(message.getContentPartsJson())))
                .toList();
        return new ConversationState(history);
    }

    @Transactional
    @Override
    public void saveExchange(AiConversationExchange exchange) {
        Require.notNull(exchange, AiCode.CHAT_CONTEXT_UNAVAILABLE, "会话交换内容不能为空");
        AiConversationScope scope = exchange.scope();
        List<AiMessageContentPartVO> userContentParts = exchange.userContentParts();
        List<AiMessageContentPartVO> assistantContentParts = exchange.assistantContentParts();
        boolean thinkingEnabled = exchange.thinkingEnabled();
        AiModelResolution resolution = exchange.resolution();
        AiChatConversationEntity conversation = findConversation(scope);
        int nextSequence;
        if (conversation == null) {
            conversation = new AiChatConversationEntity();
            conversation.setId(IdWorker.getId());
            conversation.setTenantId(scope.tenantId());
            conversation.setUserId(scope.userId());
            conversation.setServiceCode(scope.serviceCode());
            conversation.setSessionId(scope.sessionId());
            conversation.setTitle(title(userContentParts));
            conversation.setLastModelId(resolution.getModelId());
            conversation.setLastModelName(resolution.getModelName());
            conversation.setLastProviderCode(resolution.getProviderCode());
            conversation.setLastThinkingEnabled(thinkingEnabled);
            conversation.setMessageCount(2);
            Require.isTrue(conversationMapper.insert(conversation) > 0, AiCode.CHAT_CONTEXT_UNAVAILABLE);
            nextSequence = 1;
        } else {
            Integer currentCount = Require.nonNull(
                    conversation.getMessageCount(), AiCode.CHAT_CONTEXT_UNAVAILABLE, "会话消息计数缺失");
            int nextCount = currentCount + 2;
            Require.isTrue(conversationMapper.update(null, new LambdaUpdateWrapper<AiChatConversationEntity>()
                            .eq(AiChatConversationEntity::getId, conversation.getId())
                            .eq(AiChatConversationEntity::getMessageCount, currentCount)
                            .set(AiChatConversationEntity::getMessageCount, nextCount)
                            .set(AiChatConversationEntity::getLastModelId, resolution.getModelId())
                            .set(AiChatConversationEntity::getLastModelName, resolution.getModelName())
                            .set(AiChatConversationEntity::getLastProviderCode, resolution.getProviderCode())
                            .set(AiChatConversationEntity::getLastThinkingEnabled, thinkingEnabled)) > 0,
                    AiCode.CHAT_CONTEXT_UNAVAILABLE, "当前会话正在处理其他消息，请稍后重试");
            nextSequence = currentCount + 1;
        }
        insertMessage(conversation, nextSequence, "user", userContentParts, scope.userId(), thinkingEnabled, resolution);
        insertMessage(conversation, nextSequence + 1, "assistant", assistantContentParts, scope.userId(),
                thinkingEnabled, resolution);
    }

    @Transactional
    @Override
    public boolean delete(AiConversationScope scope) {
        Require.notNull(scope, AiCode.CHAT_REQUEST_INVALID, "会话定位信息不能为空");
        AiChatConversationEntity conversation = requireConversation(scope);
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getConversationId, conversation.getId()));
        return conversationMapper.deleteById(conversation.getId()) > 0;
    }

    private AiChatConversationEntity requireConversation(AiConversationScope scope) {
        return Require.nonNull(findConversation(scope),
                AiCode.CHAT_CONVERSATION_NOT_FOUND);
    }

    private AiChatConversationEntity findConversation(AiConversationScope scope) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<AiChatConversationEntity>()
                .eq(AiChatConversationEntity::getTenantId, scope.tenantId())
                .eq(AiChatConversationEntity::getUserId, scope.userId())
                .eq(AiChatConversationEntity::getServiceCode, scope.serviceCode())
                .eq(AiChatConversationEntity::getSessionId, scope.sessionId()));
    }

    private List<AiChatMessageEntity> messages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getConversationId, conversationId)
                .orderByAsc(AiChatMessageEntity::getSequenceNo));
    }

    private void insertMessage(
            AiChatConversationEntity conversation,
            int sequence,
            String role,
            List<AiMessageContentPartVO> contentParts,
            Long userId,
            boolean thinkingEnabled,
            AiModelResolution resolution) {
        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setId(IdWorker.getId());
        message.setTenantId(conversation.getTenantId());
        message.setCreatedBy(userId);
        message.setUpdatedBy(userId);
        message.setConversationId(conversation.getId());
        message.setSequenceNo(sequence);
        message.setRole(role);
        message.setContentPartsJson(writeParts(contentParts));
        if ("assistant".equals(role)) {
            message.setModelId(resolution.getModelId());
            message.setModelName(resolution.getModelName());
            message.setProviderCode(resolution.getProviderCode());
            message.setThinkingEnabled(thinkingEnabled);
        }
        Require.isTrue(messageMapper.insert(message) > 0, AiCode.CHAT_CONTEXT_UNAVAILABLE);
    }

    private AiChatConversationVO toSummary(AiChatConversationEntity entity) {
        AiChatConversationVO summary = new AiChatConversationVO();
        copySummary(entity, summary);
        return summary;
    }

    private void copySummary(AiChatConversationEntity entity, AiChatConversationVO summary) {
        summary.setSessionId(entity.getSessionId());
        summary.setTitle(entity.getTitle());
        summary.setLastModelId(entity.getLastModelId());
        summary.setLastModelName(entity.getLastModelName());
        summary.setLastProviderCode(entity.getLastProviderCode());
        summary.setLastThinkingEnabled(entity.getLastThinkingEnabled());
        summary.setMessageCount(entity.getMessageCount());
        summary.setUpdatedAt(entity.getUpdatedAt());
    }

    private AiChatMessageVO toMessage(AiChatMessageEntity entity) {
        AiChatMessageVO message = new AiChatMessageVO();
        message.setRole(entity.getRole());
        message.setContentParts(readParts(entity.getContentPartsJson()));
        message.setModelId(entity.getModelId());
        message.setModelName(entity.getModelName());
        message.setProviderCode(entity.getProviderCode());
        message.setThinkingEnabled(entity.getThinkingEnabled());
        message.setCreatedAt(entity.getCreatedAt());
        return message;
    }

    private String title(List<AiMessageContentPartVO> parts) {
        String source = parts.stream()
                .filter(part -> part.getText() != null && !part.getText().isBlank())
                .map(AiMessageContentPartVO::getText)
                .findFirst()
                .orElseGet(() -> parts.stream()
                        .filter(part -> part.getFileName() != null && !part.getFileName().isBlank())
                        .map(AiMessageContentPartVO::getFileName)
                        .findFirst()
                        .orElse("新对话"));
        String normalized = source.replaceAll("\\s+", " ").trim();
        int codePoints = normalized.codePointCount(0, normalized.length());
        if (codePoints <= MAX_TITLE_CODE_POINTS) {
            return normalized;
        }
        int end = normalized.offsetByCodePoints(0, MAX_TITLE_CODE_POINTS);
        return normalized.substring(0, end) + "…";
    }

    private String writeParts(List<AiMessageContentPartVO> parts) {
        Require.isTrue(parts != null && !parts.isEmpty(), AiCode.CHAT_CONTEXT_UNAVAILABLE, "消息内容不能为空");
        try {
            return objectMapper.writeValueAsString(parts);
        } catch (JsonProcessingException exception) {
            return Require.rethrow(new IllegalStateException("AI 消息内容序列化失败", exception));
        }
    }

    private List<AiMessageContentPartVO> readParts(String value) {
        Require.notBlank(value, AiCode.CHAT_CONTEXT_UNAVAILABLE, "AI 消息内容缺失");
        try {
            List<AiMessageContentPartVO> parts = objectMapper.readValue(value, CONTENT_PARTS_TYPE);
            Require.isTrue(parts != null && !parts.isEmpty(), AiCode.CHAT_CONTEXT_UNAVAILABLE, "AI 消息内容为空");
            return List.copyOf(parts);
        } catch (JsonProcessingException exception) {
            return Require.fail(AiCode.CHAT_CONTEXT_UNAVAILABLE, "AI 消息内容损坏", exception);
        }
    }

}
