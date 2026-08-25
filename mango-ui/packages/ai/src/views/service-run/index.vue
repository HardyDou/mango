<template>
  <!-- mango-page-baseline-exception all: AI 运行台是沉浸式多轮会话工作台，不是管理列表、详情、表单或标准弹框 -->
  <MangoListPage class="mango-ai-service-run" data-page="ai.service-run">
    <el-alert v-if="pageError" :title="pageError" type="error" :closable="false" show-icon data-state="page-error" />

    <div v-else v-loading="loading" class="mango-ai-workbench is-chat">
      <header class="mango-ai-workbench__header" data-surface="ai.service-run.header">
        <div class="mango-ai-workbench__identity">
          <el-button link data-action="ai.service-run.back" @click="backToServices">
            <el-icon><Back /></el-icon>AI 服务
          </el-button>
          <div class="mango-ai-workbench__mark">AI</div>
          <div class="mango-ai-workbench__title">
            <div>
              <strong>{{ service?.name || 'AI 工作台' }}</strong>
              <el-tag v-if="service" size="small" effect="plain">{{ serviceTypeLabel(service.serviceType) }}</el-tag>
            </div>
            <small>{{ service?.description || service?.code || '正在加载服务定义' }}</small>
          </div>
        </div>

        <div v-if="service" class="mango-ai-workbench__settings" data-surface="ai.service-run.context">
          <div class="mango-ai-workbench__context">
            <span v-if="service.promptName">Prompt · {{ service.promptName }}</span>
            <el-popover v-if="activeSkill" placement="bottom" :width="420" trigger="click">
              <template #reference>
                <button type="button" class="mango-ai-workbench__skill" data-action="ai.service-run.skill-detail">
                  Skill · {{ activeSkill.name }}
                </button>
              </template>
              <div class="mango-ai-workbench__skill-detail">
                <strong>{{ activeSkill.name }}</strong>
                <p>{{ activeSkill.description || '该 Skill 由当前服务自动加载。' }}</p>
                <pre>{{ activeSkill.instructions }}</pre>
              </div>
            </el-popover>
          </div>
        </div>
      </header>

      <AiConversationWorkspace
        v-if="service"
        ref="conversationWorkspace"
        :conversations="conversations"
        :active-conversation-id="activeConversationId"
        :active-conversation="activeConversation"
        :sending="sending"
        :conversation-loading="conversationLoading"
        :welcome-title="welcomeTitle"
        :welcome-description="service.description || welcomeDescription"
        :suggestions="suggestions"
        @create="newConversation"
        @select="selectConversation"
        @delete="deleteConversation"
        @suggestion="useSuggestion"
      >
        <template #message="{ message }">
          <ChatMessageParts :parts="message.contentParts" :streaming="Boolean(message.generationStatus)" />
          <div v-if="messageText(message)" class="mango-ai-chat__message-actions">
            <el-button link data-action="ai.service-run.copy" @click="copyText(messageText(message))">
              <el-icon><CopyDocument /></el-icon>复制
            </el-button>
          </div>
        </template>
        <template #composer>
          <el-alert
            v-if="sendError"
            :title="sendError"
            :type="sendErrorType"
            :closable="false"
            show-icon
            data-state="send-error"
          />
          <AttachmentIntake :disabled="attachmentIntakeDisabled" :hint="attachmentIntakeHint" @files="queueAttachments">
            <div class="mango-ai-chat__composer" data-surface="ai.service-run.composer">
              <div
                v-if="pendingAttachments.length"
                class="mango-ai-chat__attachments"
                data-surface="ai.service-run.attachments"
              >
                <div
                  v-for="attachment in pendingAttachments"
                  :key="attachment.id"
                  class="mango-ai-chat__attachment"
                  :class="`is-${attachment.status}`"
                >
                  <PendingAttachmentPreview :file="attachment.file" :type="attachment.type" />
                  <div class="mango-ai-chat__attachment-detail">
                    <strong>{{ attachment.file.name }}</strong
                    ><small>{{ attachmentStatus(attachment) }}</small>
                  </div>
                  <div class="mango-ai-chat__attachment-actions">
                    <el-tooltip v-if="attachment.status === 'error'" content="重新上传" placement="top">
                      <el-button
                        link
                        :aria-label="`重新上传 ${attachment.file.name}`"
                        data-action="ai.service-run.attachment-retry"
                        @click="retryAttachment(attachment.id)"
                        ><el-icon><RefreshRight /></el-icon
                      ></el-button>
                    </el-tooltip>
                    <el-button
                      link
                      :aria-label="`移除 ${attachment.file.name}`"
                      data-action="ai.service-run.attachment-remove"
                      @click="removeAttachment(attachment.id)"
                      ><el-icon><Close /></el-icon
                    ></el-button>
                  </div>
                  <el-progress
                    v-if="attachment.status === 'uploading'"
                    :percentage="attachment.progress"
                    :show-text="false"
                  />
                </div>
              </div>
              <el-input
                ref="composerInput"
                v-model="draft"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 9 }"
                maxlength="20000"
                resize="none"
                :placeholder="composerPlaceholder"
                aria-label="消息内容"
                data-field="ai.service-run.message"
                :disabled="!service.enabled || conversationLoading || sending"
                @keydown="handleKeydown"
              />
              <div class="mango-ai-chat__composer-footer">
                <div class="mango-ai-chat__composer-tools">
                  <input
                    ref="fileInput"
                    class="mango-ai-chat__file-input"
                    type="file"
                    multiple
                    :accept="attachmentSupportValue.accept"
                    @change="filesSelected"
                  />
                  <el-tooltip :content="attachmentTooltip" placement="top">
                    <el-button
                      circle
                      aria-label="添加附件"
                      data-action="ai.service-run.attach"
                      :disabled="sending || !attachmentSupportValue.enabled || pendingAttachments.length >= 6"
                      @click="openFilePicker"
                      ><el-icon><Paperclip /></el-icon
                    ></el-button>
                  </el-tooltip>
                  <AiComposerControls
                    v-model:thinking-enabled="thinkingEnabled"
                    :model-value="selectedModelId"
                    :models="runtimeOptions?.models || []"
                    :thinking-tooltip="thinkingTooltip"
                    @model-change="modelChanged"
                  />
                  <span class="mango-ai-chat__keyboard-hint">Enter 发送 · Shift + Enter 换行</span>
                </div>
                <el-tooltip v-if="sending" content="停止生成" placement="top">
                  <el-button circle type="primary" aria-label="停止生成" data-action="ai.service-run.stop" @click="stop"
                    ><span class="mango-ai-chat__stop-icon"
                  /></el-button>
                </el-tooltip>
                <el-tooltip v-else content="发送消息" placement="top">
                  <el-button
                    v-auth="'ai:service:invoke'"
                    circle
                    type="primary"
                    aria-label="发送消息"
                    data-action="ai.service-run.send"
                    :disabled="!canSend"
                    @click="send()"
                    ><el-icon><Position /></el-icon
                  ></el-button>
                </el-tooltip>
              </div>
            </div>
          </AttachmentIntake>
          <span class="mango-ai-chat__generation-status" role="status" aria-live="polite">{{
            sending ? 'AI 正在生成回答' : sendError
          }}</span>
          <small
            >当前模型输入：{{
              currentInputFormats
            }}；消息可展示：文本、富文本、结构化数据、图片、视频、音频和文件。附件只保存 Mango 文件 ID。</small
          >
          <small>AI 可能会犯错，请核对重要信息。</small>
        </template>
      </AiConversationWorkspace>
    </div>
  </MangoListPage>
</template>

<script setup lang="ts">
import type {
  AiChatConversation,
  AiChatMessage,
  AiMessageContentPart,
  AiMessageContentPartCommand,
  AiService,
  AiServiceChatEvent,
  AiServiceRuntimeOptions,
  AiServiceType,
  AiSkill,
} from '@mango/ai-api';
import { Back, Close, CopyDocument, Paperclip, Position, RefreshRight } from '@element-plus/icons-vue';
import { MangoListPage } from '@mango/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAiConfigurationApi } from '../../composables/useAiConfigurationApi';
import { isRequestAborted, requestErrorMessage } from '../../utils/requestError';
import { attachmentSupport, validateAttachment, type AttachmentFileType } from './attachmentSupport';
import { createAttachmentUploader, type PendingAttachment } from './attachmentUploader';
import { createSmoothTextStream, finalTextRemainder, type SmoothTextStream } from './smoothStream';
import AiConversationWorkspace from '../../components/AiConversationWorkspace.vue';
import AiComposerControls from './AiComposerControls.vue';
import AttachmentIntake from './AttachmentIntake.vue';
import ChatMessageParts from './ChatMessageParts.vue';
import PendingAttachmentPreview from './PendingAttachmentPreview.vue';

defineOptions({ name: 'AiServiceRunView' });

type MessageRole = 'user' | 'assistant' | 'thinking';
interface ChatMessage {
  id: string;
  role: MessageRole;
  contentParts: AiMessageContentPart[];
  reasoning?: string;
  modelId?: string;
  modelName?: string;
  providerCode?: string;
  thinkingEnabled?: boolean;
  generationStatus?: 'connecting' | 'thinking' | 'responding' | 'finalizing';
}
interface TurnSettings {
  modelId: string;
  modelName: string;
  providerCode: string;
  thinkingEnabled: boolean;
}
interface ChatConversation {
  id: string;
  sessionId?: string;
  title: string;
  messages: ChatMessage[];
  nextModelId: string;
  nextThinkingEnabled: boolean;
  persisted: boolean;
  loaded: boolean;
  messageCount: number;
  updatedAt?: string;
}
const route = useRoute();
const router = useRouter();
const api = useAiConfigurationApi();
const service = ref<AiService>();
const skills = ref<AiSkill[]>([]);
const runtimeOptions = ref<AiServiceRuntimeOptions>();
const conversations = ref<ChatConversation[]>([]);
const activeConversationId = ref('');
const conversationWorkspace = ref<{ scrollToBottom: (force?: boolean) => Promise<void> }>();
const composerInput = ref<{ focus: () => void }>();
const fileInput = ref<HTMLInputElement>();
const draft = ref('');
const pendingAttachments = ref<PendingAttachment[]>([]);
const loading = ref(true);
const conversationLoading = ref(false);
const sending = ref(false);
const pageError = ref('');
const sendError = ref('');
const sendErrorType = ref<'error' | 'warning'>('error');
const preferredModelId = ref('');
let loadController: AbortController | undefined;
let streamController: AbortController | undefined;
let messageSequence = 0;
let conversationSequence = 0;
let attachmentSequence = 0;
const attachmentUploader = createAttachmentUploader(
  (file, onProgress, signal) => api.uploadChatFile(file, onProgress, signal),
  (error) => requestErrorMessage(error, '上传失败'),
);

const serviceCode = computed(() => (typeof route.query.serviceCode === 'string' ? route.query.serviceCode : ''));
const activeSkill = computed(() => skills.value.find((item) => item.id === service.value?.skillId));
const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedModelId = computed(() => activeConversation.value?.nextModelId || '');
const thinkingEnabled = computed({
  get: () => Boolean(activeConversation.value?.nextThinkingEnabled),
  set: (value: boolean) => {
    if (activeConversation.value) activeConversation.value.nextThinkingEnabled = value;
  },
});
const selectedModel = computed(() =>
  runtimeOptions.value?.models.find((item) => item.modelId === selectedModelId.value),
);
const attachmentsReady = computed(() => pendingAttachments.value.every((item) => item.status === 'ready'));
const hasMessageContent = computed(() =>
  Boolean(draft.value.trim() || pendingAttachments.value.some((item) => item.record)),
);
const canSend = computed(() =>
  Boolean(
    service.value?.enabled &&
    selectedModelId.value &&
    hasMessageContent.value &&
    attachmentsReady.value &&
    !conversationLoading.value,
  ),
);
const attachmentSupportValue = computed(() => attachmentSupport(selectedModel.value?.inputModalities));
const currentInputFormats = computed(() => ['文本', ...attachmentSupportValue.value.labels].join('、'));
const attachmentTooltip = computed(() =>
  attachmentSupportValue.value.enabled
    ? `添加${attachmentSupportValue.value.labels.join('、')}`
    : '当前模型仅支持直接输入文本',
);
const attachmentIntakeDisabled = computed(
  () => sending.value || conversationLoading.value || !attachmentSupportValue.value.enabled,
);
const attachmentIntakeHint = computed(() =>
  attachmentSupportValue.value.enabled
    ? `支持${attachmentSupportValue.value.labels.join('、')}，最多6个附件`
    : '当前模型不支持附件输入',
);
const composerPlaceholder = computed(() =>
  attachmentSupportValue.value.enabled
    ? `给 AI 发送消息，或添加${attachmentSupportValue.value.labels.join('、')}`
    : '给 AI 发送消息',
);
const thinkingTooltip = computed(() => {
  if (!selectedModel.value) return '请先选择模型';
  if (!selectedModel.value.thinkingConfigurable) return '当前模型不支持在运行时切换思考模式';
  if (sending.value) return '调整将在下一条消息生效，不影响当前生成';
  return thinkingEnabled.value ? '下一条消息使用中等推理强度' : '下一条消息关闭额外推理';
});
const welcomeTitle = computed(() =>
  service.value?.serviceType === 'CHAT' ? '有什么可以帮忙的？' : '发送内容，开始处理',
);
const welcomeDescription = computed(() =>
  service.value?.serviceType === 'CHAT'
    ? '输入问题，开始与该 AI 服务对话。'
    : '直接粘贴文本或上传文件，结果会在对话中返回。',
);
const suggestions = computed(() =>
  service.value?.serviceType === 'CHAT'
    ? [
        { title: '介绍你的能力', description: '了解当前服务适合处理什么问题', value: '请介绍一下你能提供哪些帮助。' },
        {
          title: '分析并制定步骤',
          description: '把复杂问题拆成可执行行动',
          value: '请帮我分析这个问题，并给出清晰、可执行的步骤。',
        },
      ]
    : [
        {
          title: '粘贴待处理文本',
          description: '直接输入内容，服务会按固定 Schema 返回结果',
          value: '请处理以下内容：\n',
        },
        ...(attachmentSupportValue.value.enabled
          ? [
              {
                title: '上传业务文件',
                description: `当前模型支持${attachmentSupportValue.value.labels.join('、')}`,
                value: '',
              },
            ]
          : []),
      ],
);

async function loadService() {
  loadController?.abort();
  streamController?.abort('服务已切换');
  resetPageState();
  if (!serviceCode.value) {
    pageError.value = '缺少 AI 服务编码，请从 AI 服务列表进入工作台';
    loading.value = false;
    return;
  }
  loadController = new AbortController();
  loading.value = true;
  try {
    const [services, availableSkills, options] = await Promise.all([
      api.services(loadController.signal),
      api.skills(loadController.signal),
      api.serviceRuntimeOptions(serviceCode.value, loadController.signal),
    ]);
    const selected = services.find((item: AiService) => item.code === serviceCode.value);
    if (!selected) throw new Error('AI 服务不存在或无权访问');
    if (!selected.enabled) throw new Error('该 AI 服务已停用');
    if (!options.models.length) throw new Error('该 AI 服务没有可用模型');
    service.value = selected;
    skills.value = availableSkills;
    runtimeOptions.value = options;
    preferredModelId.value = options.defaultModelId;
    await loadConversationList(selected.code, loadController.signal);
  } catch (error) {
    if (!isRequestAborted(error)) pageError.value = requestErrorMessage(error, '加载 AI 服务失败');
  } finally {
    loading.value = false;
  }
}

function resetPageState() {
  service.value = undefined;
  skills.value = [];
  runtimeOptions.value = undefined;
  preferredModelId.value = '';
  conversations.value = [];
  activeConversationId.value = '';
  draft.value = '';
  clearPendingAttachments();
  pageError.value = '';
  sendError.value = '';
  sendErrorType.value = 'error';
}

async function loadConversationList(code: string, signal?: AbortSignal) {
  const summaries = await api.serviceConversations(code, signal);
  conversations.value = summaries.map(conversationFromSummary);
  if (!conversations.value.length) newConversation();
  else await selectConversation(conversations.value[0].id);
}

function conversationFromSummary(summary: AiChatConversation): ChatConversation {
  return {
    id: summary.sessionId,
    sessionId: summary.sessionId,
    title: summary.title,
    messages: [],
    nextModelId: String(summary.lastModelId),
    nextThinkingEnabled: summary.lastThinkingEnabled,
    persisted: true,
    loaded: false,
    messageCount: summary.messageCount,
    updatedAt: summary.updatedAt,
  };
}

function newConversation() {
  if (!runtimeOptions.value || sending.value) return;
  const draftConversation = conversations.value.find((item) => !item.persisted && !item.messages.length);
  if (draftConversation) {
    activeConversationId.value = draftConversation.id;
    return;
  }
  conversationSequence += 1;
  const modelId = runtimeOptions.value.models.some((item) => item.modelId === preferredModelId.value)
    ? preferredModelId.value
    : runtimeOptions.value.defaultModelId;
  const model = runtimeOptions.value.models.find((item) => item.modelId === modelId);
  const conversation: ChatConversation = {
    id: `draft-${Date.now()}-${conversationSequence}`,
    title: '新对话',
    messages: [],
    nextModelId: modelId,
    nextThinkingEnabled: Boolean(model?.thinkingConfigurable),
    persisted: false,
    loaded: true,
    messageCount: 0,
  };
  conversations.value.unshift(conversation);
  activeConversationId.value = conversation.id;
  draft.value = '';
  clearPendingAttachments();
  sendError.value = '';
  awaitComposerFocus();
}

async function selectConversation(id: string) {
  if (sending.value) return;
  activeConversationId.value = id;
  draft.value = '';
  clearPendingAttachments();
  sendError.value = '';
  const conversation = activeConversation.value;
  if (!conversation || conversation.loaded || !conversation.sessionId || !service.value) {
    await scrollToBottom(true);
    awaitComposerFocus();
    return;
  }
  conversationLoading.value = true;
  try {
    const detail = await api.serviceConversation(service.value.code, conversation.sessionId);
    conversation.messages = detail.messages.map((message: AiChatMessage) => createMessage(
      message.role,
      message.contentParts,
      undefined,
      message.role === 'assistant' && message.modelId != null && message.modelName
        ? {
            modelId: String(message.modelId),
            modelName: message.modelName,
            providerCode: message.providerCode || '',
            thinkingEnabled: Boolean(message.thinkingEnabled),
          }
        : undefined,
    ));
    conversation.loaded = true;
    conversation.messageCount = detail.messageCount;
  } catch (error) {
    sendError.value = requestErrorMessage(error, '加载会话失败');
  } finally {
    conversationLoading.value = false;
    await scrollToBottom(true);
    awaitComposerFocus();
  }
}

async function deleteConversation(conversation: ChatConversation) {
  if (!service.value || !conversation.sessionId) return;
  try {
    await ElMessageBox.confirm(`删除“${conversation.title}”及全部消息？`, '删除对话', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await api.deleteServiceConversation(service.value.code, conversation.sessionId);
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id);
    if (activeConversationId.value === conversation.id) {
      if (conversations.value.length) await selectConversation(conversations.value[0].id);
      else newConversation();
    }
    ElMessage.success('对话已删除');
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '删除对话失败'));
  }
}

async function send() {
  const conversation = activeConversation.value;
  if (!canSend.value || sending.value || !service.value || !conversation) return;
  const text = draft.value.trim();
  const sentAttachments = pendingAttachments.value;
  const commandParts = buildCommandParts(text, sentAttachments);
  const userParts = buildDisplayParts(text, sentAttachments);
  const turnSettings = currentTurnSettings();
  if (!turnSettings) return;
  const previousTitle = conversation.title;
  const user = createMessage('user', userParts);
  const thinking = turnSettings.thinkingEnabled ? createMessage('thinking', [], '') : undefined;
  const assistant = createMessage('assistant', [{ type: 'RICH_TEXT', text: '' }], undefined, turnSettings);
  assistant.generationStatus = 'connecting';
  let completedEvent: Extract<AiServiceChatEvent, { type: 'done' }> | undefined;
  let receivedAnswerText = '';
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const answerStream = createSmoothTextStream(
    (value) => {
      assistant.contentParts = [{ type: 'RICH_TEXT', text: value }];
      void scrollToBottom();
    },
    { reducedMotion },
  );
  const thinkingStream = createSmoothTextStream(
    (value) => {
      if (thinking) thinking.reasoning = value;
      void scrollToBottom();
    },
    { reducedMotion },
  );
  draft.value = '';
  clearPendingAttachments();
  sendError.value = '';
  conversation.messages.push(user);
  if (conversation.title === '新对话') conversation.title = conversationTitle(text, sentAttachments);
  conversation.messages.push(assistant);
  sending.value = true;
  streamController = new AbortController();
  await scrollToBottom(true);
  try {
    await api.streamServiceChat(
      service.value.code,
      {
        contentParts: commandParts,
        sessionId: conversation.sessionId,
        modelId: turnSettings.modelId,
        thinkingEnabled: turnSettings.thinkingEnabled,
      },
      (event) => {
        if (event.type === 'message') receivedAnswerText += event.content;
        completedEvent =
          handleEvent(conversation, event, thinking, assistant, answerStream, thinkingStream) ?? completedEvent;
      },
      streamController.signal,
    );
    if (!completedEvent) throw new Error('AI 服务没有完整返回本次回答');
    assistant.generationStatus = 'finalizing';
    answerStream.push(finalTextRemainder(receivedAnswerText, responseText(completedEvent.contentParts)));
    await Promise.all([answerStream.complete(), thinkingStream.complete()]);
    assistant.contentParts = completedEvent.contentParts;
    assistant.modelId = String(completedEvent.modelId);
    assistant.modelName = completedEvent.modelName;
    assistant.providerCode = completedEvent.providerCode;
    assistant.thinkingEnabled = completedEvent.thinkingEnabled;
    assistant.generationStatus = undefined;
    markConversationPersisted(conversation, completedEvent.sessionId);
  } catch (error) {
    answerStream.cancel();
    thinkingStream.cancel();
    rollbackTurn(conversation, previousTitle, text, sentAttachments, user, thinking, assistant);
    if (isRequestAborted(error)) {
      sendErrorType.value = 'warning';
      sendError.value = '已停止生成，本次消息未保存，可修改后重新发送。';
    } else {
      sendErrorType.value = 'error';
      sendError.value = requestErrorMessage(error, '当前模型暂时不可用，请检查模型和附件能力后重试。');
    }
  } finally {
    sending.value = false;
    streamController = undefined;
    await scrollToBottom(true);
    awaitComposerFocus();
  }
}

function handleEvent(
  conversation: ChatConversation,
  event: AiServiceChatEvent,
  thinking: ChatMessage | undefined,
  assistant: ChatMessage,
  answerStream: SmoothTextStream,
  thinkingStream: SmoothTextStream,
): Extract<AiServiceChatEvent, { type: 'done' }> | undefined {
  if (event.type === 'thinking' && thinking) {
    if (!conversation.messages.some((item) => item.id === thinking.id)) {
      const index = conversation.messages.findIndex((item) => item.id === assistant.id);
      conversation.messages.splice(index, 0, thinking);
    }
    assistant.generationStatus = 'thinking';
    thinkingStream.push(event.content);
  }
  if (event.type === 'message') {
    assistant.generationStatus = 'responding';
    answerStream.push(event.content);
  }
  if (event.type === 'done') {
    assistant.generationStatus = 'finalizing';
    return event;
  }
  if (event.type === 'error') throw new Error(event.message);
  return undefined;
}

function markConversationPersisted(conversation: ChatConversation, sessionId: string) {
  const previousId = conversation.id;
  conversation.sessionId = sessionId;
  conversation.id = sessionId;
  conversation.persisted = true;
  conversation.loaded = true;
  conversation.messageCount = conversation.messages.filter((item) => item.role !== 'thinking').length;
  conversation.updatedAt = new Date().toISOString();
  if (activeConversationId.value === previousId) activeConversationId.value = sessionId;
  conversations.value = [conversation, ...conversations.value.filter((item) => item !== conversation)];
}

function buildCommandParts(text: string, attachments: PendingAttachment[]): AiMessageContentPartCommand[] {
  const parts: AiMessageContentPartCommand[] = [];
  if (text) parts.push({ type: 'TEXT', text });
  attachments.forEach((item) => {
    if (item.record) parts.push({ type: item.type, fileId: item.record.id });
  });
  return parts;
}

function buildDisplayParts(text: string, attachments: PendingAttachment[]): AiMessageContentPart[] {
  const parts: AiMessageContentPart[] = [];
  if (text) parts.push({ type: 'TEXT', text });
  attachments.forEach((item) => {
    if (item.record)
      parts.push({
        type: item.type,
        fileId: item.record.id,
        fileName: item.record.fileName,
        fileSize: item.record.fileSize,
        contentType: item.record.contentType,
      });
  });
  return parts;
}

function createMessage(
  role: MessageRole,
  contentParts: AiMessageContentPart[],
  reasoning?: string,
  settings?: TurnSettings,
): ChatMessage {
  messageSequence += 1;
  return reactive<ChatMessage>({
    id: `${Date.now()}-${messageSequence}`,
    role,
    contentParts,
    reasoning,
    modelId: settings?.modelId,
    modelName: settings?.modelName,
    providerCode: settings?.providerCode,
    thinkingEnabled: settings?.thinkingEnabled,
  });
}

function currentTurnSettings(): TurnSettings | undefined {
  const model = selectedModel.value;
  if (!model) {
    sendError.value = '请先选择本轮使用的模型';
    return undefined;
  }
  if (thinkingEnabled.value && !model.thinkingConfigurable) {
    sendError.value = '当前模型不支持深度思考，请调整后重试';
    return undefined;
  }
  return {
    modelId: model.modelId,
    modelName: model.modelName,
    providerCode: model.providerCode,
    thinkingEnabled: thinkingEnabled.value,
  };
}

function rollbackTurn(
  conversation: ChatConversation,
  previousTitle: string,
  text: string,
  attachments: PendingAttachment[],
  ...items: Array<ChatMessage | undefined>
) {
  const ids = new Set(items.filter(Boolean).map((item) => item?.id));
  conversation.messages = conversation.messages.filter((item) => !ids.has(item.id));
  conversation.title = previousTitle;
  if (activeConversation.value === conversation) {
    draft.value = text;
    pendingAttachments.value = attachments;
  }
}

function openFilePicker() {
  fileInput.value?.click();
}
function filesSelected(event: Event) {
  const input = event.target as HTMLInputElement;
  queueAttachments(Array.from(input.files || []));
  input.value = '';
}

function queueAttachments(files: File[]) {
  let available = Math.max(0, 6 - pendingAttachments.value.length);
  let totalBytes = pendingAttachments.value.reduce((sum, item) => sum + item.file.size, 0);
  const fingerprints = new Set(pendingAttachments.value.map((item) => attachmentFingerprint(item.file)));
  let exceededCount = false;
  files.forEach((file) => {
    const fingerprint = attachmentFingerprint(file);
    if (fingerprints.has(fingerprint)) {
      ElMessage.warning(`文件“${file.name}”已经添加`);
      return;
    }
    if (available <= 0) {
      exceededCount = true;
      return;
    }
    const validation = validateAttachment(file, selectedModel.value?.inputModalities || [], totalBytes);
    if (!validation.accepted) {
      ElMessage.warning(validation.message);
      return;
    }
    fingerprints.add(fingerprint);
    available -= 1;
    totalBytes += file.size;
    void uploadAttachment(file, validation.partType);
  });
  if (exceededCount) ElMessage.warning('每条消息最多添加6个附件');
}

async function uploadAttachment(file: File, type: AttachmentFileType) {
  attachmentSequence += 1;
  const attachment = reactive<PendingAttachment>({
    id: `${Date.now()}-${attachmentSequence}`,
    file,
    type,
    status: 'uploading',
    progress: 0,
  });
  pendingAttachments.value.push(attachment);
  await attachmentUploader.start(attachment);
}

function retryAttachment(id: string) {
  const attachment = pendingAttachments.value.find((item) => item.id === id);
  if (!attachment || attachment.status !== 'error' || sending.value) return;
  void attachmentUploader.start(attachment);
}

function removeAttachment(id: string) {
  if (sending.value) return;
  attachmentUploader.cancel(id);
  pendingAttachments.value = pendingAttachments.value.filter((item) => item.id !== id);
}

function clearPendingAttachments() {
  attachmentUploader.cancelAll();
  pendingAttachments.value = [];
}

function attachmentFingerprint(file: File) {
  return `${file.name}\u0000${file.size}\u0000${file.lastModified}`;
}
function attachmentStatus(item: PendingAttachment) {
  if (item.status === 'uploading') return `上传中 ${item.progress}%`;
  if (item.status === 'error') return item.error || '上传失败';
  return formatBytes(item.record?.fileSize || item.file.size);
}
function formatBytes(value: number) {
  return value < 1024 * 1024 ? `${Math.max(1, Math.round(value / 1024))} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`;
}
function useSuggestion(value: string) {
  if (value) {
    draft.value = value;
    awaitComposerFocus();
  } else openFilePicker();
}
function modelChanged(modelId: string) {
  const model = runtimeOptions.value?.models.find((item) => item.modelId === modelId);
  const conversation = activeConversation.value;
  if (!model || !conversation || modelId === conversation.nextModelId) return;
  let totalBytes = 0;
  for (const attachment of pendingAttachments.value) {
    const validation = validateAttachment(attachment.file, model.inputModalities, totalBytes);
    if (!validation.accepted) {
      ElMessage.warning(`无法切换到“${model.displayName}”：${validation.message}`);
      return;
    }
    totalBytes += attachment.file.size;
  }
  conversation.nextModelId = modelId;
  if (!model.thinkingConfigurable) conversation.nextThinkingEnabled = false;
  preferredModelId.value = modelId;
  sendError.value = '';
}
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault();
    void send();
  }
}
function stop() {
  streamController?.abort('用户停止生成');
}
function messageText(message: ChatMessage) {
  return message.contentParts
    .map((part) => part.dataJson || part.text || '')
    .filter(Boolean)
    .join('\n\n');
}
function responseText(parts: AiMessageContentPart[]) {
  return parts
    .map((part) => part.text || part.dataJson || '')
    .filter(Boolean)
    .join('\n\n');
}
async function copyText(value: string) {
  try {
    await navigator.clipboard.writeText(value);
    ElMessage.success('已复制');
  } catch {
    ElMessage.error('复制失败，请手动选择内容');
  }
}
function conversationTitle(text: string, attachments: PendingAttachment[]) {
  const value = text || attachments[0]?.file.name || '新对话';
  const normalized = value.replace(/\s+/g, ' ').trim();
  return normalized.length > 40 ? `${normalized.slice(0, 40)}…` : normalized;
}
function serviceTypeLabel(type: AiServiceType) {
  return ({ CHAT: '对话', EXTRACTION: '信息抽取', CLASSIFICATION: '文本分类' } as const)[type];
}
async function scrollToBottom(force = false) {
  await conversationWorkspace.value?.scrollToBottom(force);
}
function awaitComposerFocus() {
  void nextTick(() => composerInput.value?.focus());
}
async function backToServices() {
  await router.push('/ai/services');
}

watch(serviceCode, () => void loadService(), { immediate: true });
onBeforeUnmount(() => {
  loadController?.abort();
  streamController?.abort('页面已离开');
  clearPendingAttachments();
});
</script>
