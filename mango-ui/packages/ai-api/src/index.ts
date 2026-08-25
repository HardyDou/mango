import type { ApiId, HttpClient, HttpProgress, HttpQuery } from '@mango/api-schema';

export type AiProviderType =
  | 'DEEPSEEK'
  | 'VOLCENGINE_ARK'
  | 'ALIBABA_DASHSCOPE'
  | 'ZHIPU'
  | 'SILICONFLOW'
  | 'KIMI'
  | 'OPENAI_COMPATIBLE'
  | 'OLLAMA';
export type AiApiProtocol = 'CHAT_COMPLETIONS' | 'RESPONSES';
export type AiCapability =
  'CHAT' | 'EMBEDDING' | 'RERANK' | 'IMAGE_GENERATION' | 'SPEECH_TO_TEXT' | 'TEXT_TO_SPEECH' | 'VIDEO_GENERATION';
export type AiModality = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO' | 'FILE' | 'VECTOR';
export type AiMessageContentType = 'TEXT' | 'RICH_TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE' | 'STRUCTURED_DATA';
export type AiPromptStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type AiToolType = 'MCP' | 'HTTP';
export type AiServiceType = 'CHAT' | 'EXTRACTION' | 'CLASSIFICATION';

export interface AiProviderConnection {
  id: ApiId;
  code: string;
  displayName: string;
  providerType: AiProviderType;
  baseUrl: string;
  apiKeyConfigured: boolean;
  apiKeyHint?: string;
  enabled: boolean;
  modelCount: number;
  updatedAt?: string;
}
export interface AiProviderTypeOption {
  code: AiProviderType;
  name: string;
  defaultCode: string;
  defaultBaseUrl: string;
  apiKeyRequired: boolean;
}
export interface AiModel {
  id: ApiId;
  providerConnectionId: ApiId;
  modelName: string;
  displayName: string;
  platformAlias?: string;
  apiProtocol: AiApiProtocol;
  capabilities: AiCapability[];
  inputModalities: AiModality[];
  outputModalities: AiModality[];
  parameterJson?: string;
  enabled: boolean;
  callable: boolean;
  routedCapabilities: AiCapability[];
  updatedAt?: string;
}
export interface AiCapabilityRoute {
  capability: AiCapability;
  modelId: ApiId;
  modelDisplayName?: string;
  providerDisplayName?: string;
}
export interface CreateAiProviderConnectionCommand {
  code: string;
  displayName: string;
  providerType: AiProviderType;
  baseUrl: string;
  apiKey: string;
  enabled: boolean;
}
export interface UpdateAiProviderConnectionCommand extends Omit<CreateAiProviderConnectionCommand, 'apiKey'> {
  id: ApiId;
  apiKey?: string;
}
export interface CreateAiModelCommand {
  providerConnectionId: ApiId;
  modelName: string;
  displayName: string;
  platformAlias?: string;
  apiProtocol: AiApiProtocol;
  capabilities: AiCapability[];
  inputModalities: AiModality[];
  outputModalities: AiModality[];
  parameterJson?: string;
  enabled: boolean;
}
export interface UpdateAiModelCommand extends Omit<CreateAiModelCommand, 'providerConnectionId'> {
  id: ApiId;
}
export interface SetAiCapabilityRouteCommand {
  capability: AiCapability;
  modelId: ApiId;
}

export interface AiPrompt {
  id: ApiId;
  code: string;
  name: string;
  description?: string;
  template: string;
  variablesJson?: string;
  status: AiPromptStatus;
  version: number;
  publishedAt?: string;
  updatedAt?: string;
}
export interface CreateAiPromptCommand {
  code: string;
  name: string;
  description?: string;
  template: string;
  variablesJson?: string;
}
export interface UpdateAiPromptCommand extends CreateAiPromptCommand {
  id: ApiId;
}
export interface AiSkill {
  id: ApiId;
  code: string;
  name: string;
  description?: string;
  instructions: string;
  toolIds: ApiId[];
  enabled: boolean;
  updatedAt?: string;
}
export interface CreateAiSkillCommand {
  code: string;
  name: string;
  description?: string;
  instructions: string;
  toolIds: ApiId[];
  enabled: boolean;
}
export interface UpdateAiSkillCommand extends CreateAiSkillCommand {
  id: ApiId;
}
export interface AiTool {
  id: ApiId;
  code: string;
  name: string;
  description?: string;
  toolType: AiToolType;
  endpoint: string;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
  updatedAt?: string;
}
export interface CreateAiToolCommand {
  code: string;
  name: string;
  description?: string;
  toolType: AiToolType;
  endpoint: string;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
}
export interface UpdateAiToolCommand extends CreateAiToolCommand {
  id: ApiId;
}
export interface AiService {
  id: ApiId;
  code: string;
  name: string;
  description?: string;
  serviceType: AiServiceType;
  capability?: AiCapability;
  promptId?: ApiId;
  promptName?: string;
  skillId?: ApiId;
  skillName?: string;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
  updatedAt?: string;
}
export interface CreateAiServiceCommand {
  code: string;
  name: string;
  description?: string;
  serviceType: AiServiceType;
  capability?: AiCapability;
  promptId?: ApiId;
  skillId?: ApiId;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
}
export interface UpdateAiServiceCommand extends CreateAiServiceCommand {
  id: ApiId;
}
export interface AiServiceChatCommand {
  requestId: string;
  contentParts: AiMessageContentPartCommand[];
  sessionId?: string;
  modelId: ApiId;
  thinkingEnabled: boolean;
}
export interface AiMessageContentPartCommand {
  type: Extract<AiMessageContentType, 'TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE'>;
  text?: string;
  fileId?: ApiId;
}
export interface AiMessageContentPart {
  type: AiMessageContentType;
  text?: string | null;
  dataJson?: string | null;
  fileId?: ApiId | null;
  fileName?: string | null;
  contentType?: string | null;
  fileSize?: number | null;
}
export interface AiServiceModelOption {
  modelId: ApiId;
  modelName: string;
  displayName: string;
  providerCode: string;
  providerDisplayName: string;
  apiProtocol: AiApiProtocol;
  thinkingConfigurable: boolean;
  inputModalities: AiModality[];
  outputModalities: AiModality[];
}
export interface AiServiceRuntimeOptions {
  defaultModelId: ApiId;
  models: AiServiceModelOption[];
}
export interface AiChatConversation {
  sessionId: string;
  title: string;
  lastModelId: ApiId;
  lastModelName: string;
  lastProviderCode: string;
  lastThinkingEnabled: boolean;
  messageCount: number;
  updatedAt?: string;
}
export interface AiChatMessage {
  role: 'user' | 'assistant';
  contentParts: AiMessageContentPart[];
  modelId?: ApiId | null;
  modelName?: string | null;
  providerCode?: string | null;
  thinkingEnabled?: boolean | null;
  createdAt?: string;
}
export interface AiChatConversationDetail extends AiChatConversation {
  messages: AiChatMessage[];
}
export type AiServiceChatEvent =
  | { type: 'thinking'; content: string }
  | { type: 'message'; content: string }
  | {
      type: 'done';
      sessionId: string;
      requestId: string;
      modelId: ApiId;
      modelName: string;
      providerCode: string;
      thinkingEnabled: boolean;
      contentParts: AiMessageContentPart[];
    }
  | { type: 'error'; message: string };
export interface AiServiceChatStart {
  requestId: string;
  sessionId: string;
}
export interface AiChatFileRecord {
  id: ApiId;
  fileName: string;
  fileSize: number;
  contentType?: string;
}

export function createAiModelManagementApi(httpClient: HttpClient) {
  return {
    providers: (signal?: AbortSignal) =>
      httpClient.request<AiProviderConnection[]>({ method: 'GET', url: '/ai/models/providers', signal }),
    providerTypes: (signal?: AbortSignal) =>
      httpClient.request<AiProviderTypeOption[]>({ method: 'GET', url: '/ai/models/provider-types', signal }),
    models: (providerConnectionId: ApiId, query: { keyword?: string; enabled?: boolean } = {}, signal?: AbortSignal) =>
      httpClient.request<AiModel[]>({
        method: 'GET',
        url: '/ai/models',
        query: compactQuery({ providerConnectionId, ...query }),
        signal,
      }),
    routes: (signal?: AbortSignal) =>
      httpClient.request<AiCapabilityRoute[]>({ method: 'GET', url: '/ai/models/routes', signal }),
    createProvider: (command: CreateAiProviderConnectionCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiProviderConnectionCommand>({
        method: 'POST',
        url: '/ai/models/providers',
        body: command,
        signal,
      }),
    updateProvider: (command: UpdateAiProviderConnectionCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiProviderConnectionCommand>({
        method: 'PUT',
        url: '/ai/models/providers',
        body: command,
        signal,
      }),
    deleteProvider: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/models/providers', query: { id }, signal }),
    createModel: (command: CreateAiModelCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiModelCommand>({ method: 'POST', url: '/ai/models', body: command, signal }),
    updateModel: (command: UpdateAiModelCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiModelCommand>({ method: 'PUT', url: '/ai/models', body: command, signal }),
    deleteModel: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/models', query: { id }, signal }),
    setRoute: (command: SetAiCapabilityRouteCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, SetAiCapabilityRouteCommand>({
        method: 'PUT',
        url: '/ai/models/routes',
        body: command,
        signal,
      }),
    prompts: (signal?: AbortSignal) => httpClient.request<AiPrompt[]>({ method: 'GET', url: '/ai/prompts', signal }),
    createPrompt: (command: CreateAiPromptCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiPromptCommand>({ method: 'POST', url: '/ai/prompts', body: command, signal }),
    updatePrompt: (command: UpdateAiPromptCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiPromptCommand>({ method: 'PUT', url: '/ai/prompts', body: command, signal }),
    deletePrompt: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/prompts', query: { id }, signal }),
    publishPrompt: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'PUT', url: '/ai/prompts/publish', query: { id }, signal }),
    skills: (signal?: AbortSignal) => httpClient.request<AiSkill[]>({ method: 'GET', url: '/ai/skills', signal }),
    createSkill: (command: CreateAiSkillCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiSkillCommand>({ method: 'POST', url: '/ai/skills', body: command, signal }),
    updateSkill: (command: UpdateAiSkillCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiSkillCommand>({ method: 'PUT', url: '/ai/skills', body: command, signal }),
    deleteSkill: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/skills', query: { id }, signal }),
    tools: (signal?: AbortSignal) => httpClient.request<AiTool[]>({ method: 'GET', url: '/ai/tools', signal }),
    createTool: (command: CreateAiToolCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiToolCommand>({ method: 'POST', url: '/ai/tools', body: command, signal }),
    updateTool: (command: UpdateAiToolCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiToolCommand>({ method: 'PUT', url: '/ai/tools', body: command, signal }),
    deleteTool: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/tools', query: { id }, signal }),
    services: (signal?: AbortSignal): Promise<AiService[]> =>
      httpClient.request<AiService[]>({ method: 'GET', url: '/ai/services', signal }),
    createService: (command: CreateAiServiceCommand, signal?: AbortSignal) =>
      httpClient.request<ApiId, CreateAiServiceCommand>({ method: 'POST', url: '/ai/services', body: command, signal }),
    updateService: (command: UpdateAiServiceCommand, signal?: AbortSignal) =>
      httpClient.request<boolean, UpdateAiServiceCommand>({
        method: 'PUT',
        url: '/ai/services',
        body: command,
        signal,
      }),
    deleteService: (id: ApiId, signal?: AbortSignal) =>
      httpClient.request<boolean>({ method: 'DELETE', url: '/ai/services', query: { id }, signal }),
    serviceRuntimeOptions: (serviceCode: string, signal?: AbortSignal): Promise<AiServiceRuntimeOptions> =>
      httpClient.request<AiServiceRuntimeOptions>({
        method: 'GET',
        url: '/ai/services/options',
        query: { serviceCode },
        signal,
      }),
    serviceConversations: (serviceCode: string, signal?: AbortSignal): Promise<AiChatConversation[]> =>
      httpClient.request<AiChatConversation[]>({
        method: 'GET',
        url: '/ai/services/conversations',
        query: { serviceCode },
        signal,
      }),
    serviceConversation: (
      serviceCode: string,
      sessionId: string,
      signal?: AbortSignal,
    ): Promise<AiChatConversationDetail> =>
      httpClient.request<AiChatConversationDetail>({
        method: 'GET',
        url: '/ai/services/conversation',
        query: { serviceCode, sessionId },
        signal,
      }),
    deleteServiceConversation: (serviceCode: string, sessionId: string, signal?: AbortSignal): Promise<boolean> =>
      httpClient.request<boolean>({
        method: 'DELETE',
        url: '/ai/services/conversation',
        query: { serviceCode, sessionId },
        signal,
      }),
    uploadChatFile: (
      file: File,
      onUploadProgress?: (progress: HttpProgress) => void,
      signal?: AbortSignal,
    ): Promise<AiChatFileRecord> => {
      const body = new FormData();
      body.append('file', file);
      body.append('purpose', 'ai-chat');
      body.append('accessLevel', 'PRIVATE');
      body.append('bizType', 'AI_CHAT');
      return httpClient.request<AiChatFileRecord, FormData>({
        method: 'POST',
        url: '/file/files',
        body,
        onUploadProgress,
        signal,
      });
    },
    previewChatFile: (fileId: ApiId, signal?: AbortSignal): Promise<Blob> =>
      httpClient.request<Blob>({
        method: 'GET',
        url: '/file/files/preview-content',
        query: { id: fileId },
        responseType: 'blob',
        signal,
      }),
    downloadChatFile: (fileId: ApiId, signal?: AbortSignal): Promise<Blob> =>
      httpClient.request<Blob>({
        method: 'GET',
        url: '/file/files/download',
        query: { id: fileId },
        responseType: 'blob',
        signal,
      }),
    startServiceChat: (
      serviceCode: string,
      command: AiServiceChatCommand,
      signal?: AbortSignal,
    ): Promise<AiServiceChatStart> =>
      httpClient.request<AiServiceChatStart, AiServiceChatCommand>({
        method: 'POST',
        url: '/ai/services/chat',
        query: { serviceCode },
        body: command,
        signal,
      }),
    cancelServiceChat: (requestId: string, signal?: AbortSignal): Promise<boolean> =>
      httpClient.request<boolean>({
        method: 'DELETE',
        url: '/ai/services/chat',
        query: { requestId },
        signal,
      }),
  };
}

export type AiModelManagementApi = ReturnType<typeof createAiModelManagementApi>;
function compactQuery(query: Record<string, unknown>): HttpQuery {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== ''),
  ) as HttpQuery;
}

export function parseAiServiceChatEvent(value: unknown): AiServiceChatEvent {
  const parsed: unknown = typeof value === 'string' ? JSON.parse(value) : value;
  if (!isAiServiceChatEvent(parsed)) throw new Error('AI 服务返回了无法识别的实时事件');
  return parsed;
}

function isAiServiceChatEvent(value: unknown): value is AiServiceChatEvent {
  if (!value || typeof value !== 'object' || !('type' in value)) return false;
  const event = value as Record<string, unknown>;
  if (event.type === 'thinking' || event.type === 'message') return typeof event.content === 'string';
  if (event.type === 'error') return typeof event.message === 'string';
  return (
    event.type === 'done' &&
    typeof event.sessionId === 'string' &&
    typeof event.requestId === 'string' &&
    isNullableId(event.modelId) &&
    event.modelId != null &&
    typeof event.modelName === 'string' &&
    typeof event.providerCode === 'string' &&
    typeof event.thinkingEnabled === 'boolean' &&
    Array.isArray(event.contentParts) &&
    event.contentParts.every(isAiMessageContentPart)
  );
}

function isAiMessageContentPart(value: unknown): value is AiMessageContentPart {
  if (!value || typeof value !== 'object' || !('type' in value)) return false;
  const part = value as Record<string, unknown>;
  return (
    ['TEXT', 'RICH_TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'STRUCTURED_DATA'].includes(String(part.type)) &&
    isNullableString(part.text) &&
    isNullableString(part.dataJson) &&
    isNullableId(part.fileId) &&
    isNullableString(part.fileName) &&
    isNullableString(part.contentType) &&
    isNullableNumber(part.fileSize)
  );
}

function isNullableString(value: unknown): value is string | null | undefined {
  return value == null || typeof value === 'string';
}

function isNullableId(value: unknown): value is ApiId | null | undefined {
  return value == null || typeof value === 'string' || typeof value === 'number';
}

function isNullableNumber(value: unknown): value is number | null | undefined {
  return value == null || typeof value === 'number';
}
