import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { describe, expect, it } from 'vitest';
import { createAiModelManagementApi, type AiServiceChatEvent } from './index';

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

  it('按服务编码请求并正确处理跨分片 SSE 事件', async () => {
    const encoder = new TextEncoder();
    const chunks = [
      'data: {"type":"mess',
      'age","content":"你好"}\n\ndata: {"type":"done","sessionId":"s-1",',
      '"requestId":"r-1","modelId":"100","modelName":"gpt-5.6-sol","providerCode":"openai-compatible","thinkingEnabled":false,"contentParts":[{"type":"RICH_TEXT","text":"你好"}]}\n\n',
    ];
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: async <TResponse, TBody>(request: HttpRequest<TBody>) => {
        requests.push(request);
        return new ReadableStream<Uint8Array>({
          start(controller) {
            chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
            controller.close();
          },
        }) as TResponse;
      },
    };
    const events: AiServiceChatEvent[] = [];

    await createAiModelManagementApi(client).streamServiceChat(
      'assistant/general',
      { contentParts: [{ type: 'TEXT', text: '你好' }], modelId: '100', thinkingEnabled: false },
      (event) => events.push(event),
    );

    expect(requests[0]).toMatchObject({
      method: 'POST',
      url: '/ai/services/chat',
      query: { serviceCode: 'assistant/general' },
      responseType: 'stream',
      headers: { Accept: 'text/event-stream' },
    });
    expect(events).toEqual([
      { type: 'message', content: '你好' },
      {
        type: 'done',
        sessionId: 's-1',
        requestId: 'r-1',
        modelId: '100',
        modelName: 'gpt-5.6-sol',
        providerCode: 'openai-compatible',
        thinkingEnabled: false,
        contentParts: [{ type: 'RICH_TEXT', text: '你好' }],
      },
    ]);
  });

  it('接受后端为未使用内容块字段返回 null 的真实 SSE 契约', async () => {
    const encoder = new TextEncoder();
    const client: HttpClient = {
      request: async <TResponse>() =>
        new ReadableStream<Uint8Array>({
          start(controller) {
            controller.enqueue(
              encoder.encode(
                'data:{"type":"done","sessionId":"s-1","requestId":"r-1","modelId":100,"modelName":"deepseek-chat","providerCode":"deepseek","thinkingEnabled":true,"contentParts":[{"type":"RICH_TEXT","text":"完成","dataJson":null,"fileId":null,"fileName":null,"contentType":null,"fileSize":null}]}\n\n',
              ),
            );
            controller.close();
          },
        }) as TResponse,
    };
    const events: AiServiceChatEvent[] = [];

    await createAiModelManagementApi(client).streamServiceChat(
      'assistant.general',
      { contentParts: [{ type: 'TEXT', text: '你好' }], modelId: '100', thinkingEnabled: false },
      (event) => events.push(event),
    );

    expect(events).toEqual([
      {
        type: 'done',
        sessionId: 's-1',
        requestId: 'r-1',
        modelId: 100,
        modelName: 'deepseek-chat',
        providerCode: 'deepseek',
        thinkingEnabled: true,
        contentParts: [
          {
            type: 'RICH_TEXT',
            text: '完成',
            dataJson: null,
            fileId: null,
            fileName: null,
            contentType: null,
            fileSize: null,
          },
        ],
      },
    ]);
  });

  it('拒绝非契约事件', async () => {
    const client: HttpClient = {
      request: async <TResponse>() =>
        new ReadableStream<Uint8Array>({
          start(controller) {
            controller.enqueue(new TextEncoder().encode('data: {"type":"unknown"}\n\n'));
            controller.close();
          },
        }) as TResponse,
    };

    await expect(
      createAiModelManagementApi(client).streamServiceChat(
        'assistant.general',
        { contentParts: [{ type: 'TEXT', text: 'hello' }], modelId: '100', thinkingEnabled: false },
        () => undefined,
      ),
    ).rejects.toThrow('无法识别');
  });
});
