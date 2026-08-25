import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { describe, expect, it } from 'vitest';
import { createAiModelManagementApi, parseAiServiceChatEvent } from './index';

describe('createAiModelManagementApi', () => {
  it('所有服务类型使用统一运行选项入口', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        return { defaultModelId: '100', models: [] } as TResponse;
      },
    };

    await createAiModelManagementApi(client).serviceRuntimeOptions('contract/five-elements');

    expect(requests[0]).toMatchObject({
      method: 'GET',
      url: '/ai/services/options',
      query: { serviceCode: 'contract/five-elements' },
    });
  });

  it('使用固定会话资源路径并通过 query 传递服务与会话标识', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        return (
          request.method === 'DELETE' ? true : request.url.endsWith('conversations') ? [] : { messages: [] }
        ) as TResponse;
      },
    };
    const api = createAiModelManagementApi(client);

    await api.serviceConversations('assistant.general');
    await api.serviceConversation('assistant.general', 'session-1');
    await api.deleteServiceConversation('assistant.general', 'session-1');

    expect(requests).toMatchObject([
      {
        method: 'GET',
        url: '/ai/services/conversations',
        query: { serviceCode: 'assistant.general' },
      },
      {
        method: 'GET',
        url: '/ai/services/conversation',
        query: { serviceCode: 'assistant.general', sessionId: 'session-1' },
      },
      {
        method: 'DELETE',
        url: '/ai/services/conversation',
        query: { serviceCode: 'assistant.general', sessionId: 'session-1' },
      },
    ]);
  });

  it('使用固定提示词发布路径', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        return true as TResponse;
      },
    };

    await createAiModelManagementApi(client).publishPrompt('200');

    expect(requests[0]).toMatchObject({
      method: 'PUT',
      url: '/ai/prompts/publish',
      query: { id: '200' },
    });
  });

  it('通过注入的HttpClient上传和读取AI会话文件', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        if (request.method === 'POST') {
          return { id: '10', fileName: 'note.txt', fileSize: 4, contentType: 'text/plain' } as TResponse;
        }
        return new Blob(['file']) as TResponse;
      },
    };
    const api = createAiModelManagementApi(client);
    const progress = () => undefined;

    await api.uploadChatFile(new File(['note'], 'note.txt', { type: 'text/plain' }), progress);
    await api.previewChatFile('10');
    await api.downloadChatFile('10');

    expect(requests[0]).toMatchObject({ method: 'POST', url: '/file/files', onUploadProgress: progress });
    expect(requests[0].body).toBeInstanceOf(FormData);
    expect(requests.slice(1)).toMatchObject([
      { method: 'GET', url: '/file/files/preview-content', query: { id: '10' }, responseType: 'blob' },
      { method: 'GET', url: '/file/files/download', query: { id: '10' }, responseType: 'blob' },
    ]);
  });

  it('通过标准响应受理会话调用并支持显式取消', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        return (
          request.method === 'POST' ? { requestId: '1d8f5930-87ac-4b6f-b330-6294c2b252ea', sessionId: 's-1' } : true
        ) as TResponse;
      },
    };
    const api = createAiModelManagementApi(client);
    const requestId = '1d8f5930-87ac-4b6f-b330-6294c2b252ea';

    await api.startServiceChat('assistant/general', {
      requestId,
      contentParts: [{ type: 'TEXT', text: '你好' }],
      modelId: '100',
      thinkingEnabled: false,
    });
    await api.cancelServiceChat(requestId);

    expect(requests[0]).toMatchObject({
      method: 'POST',
      url: '/ai/services/chat',
      query: { serviceCode: 'assistant/general' },
      body: { requestId },
    });
    expect(requests[1]).toMatchObject({
      method: 'DELETE',
      url: '/ai/services/chat',
      query: { requestId },
    });
  });

  it('解析 Realtime 中的真实完成事件和 null 内容块字段', () => {
    const event = parseAiServiceChatEvent(
      '{"type":"done","sessionId":"s-1","requestId":"1d8f5930-87ac-4b6f-b330-6294c2b252ea","modelId":100,"modelName":"deepseek-chat","providerCode":"deepseek","thinkingEnabled":true,"contentParts":[{"type":"RICH_TEXT","text":"完成","dataJson":null,"fileId":null,"fileName":null,"contentType":null,"fileSize":null}]}',
    );

    expect(event).toMatchObject({
      type: 'done',
      sessionId: 's-1',
      modelId: 100,
      contentParts: [{ type: 'RICH_TEXT', text: '完成' }],
    });
  });

  it('拒绝非契约 Realtime 事件', () => {
    expect(() => parseAiServiceChatEvent('{"type":"unknown"}')).toThrow('无法识别');
  });
});
