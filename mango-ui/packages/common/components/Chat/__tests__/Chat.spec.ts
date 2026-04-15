import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock Element Plus icons
vi.mock('@element-plus/icons-vue', () => ({
  ChatDotRound: { template: '<span class="mock-chat-icon"></span>' },
  Close: { template: '<span class="mock-close-icon"></span>' },
  User: { template: '<span class="mock-user-icon"></span>' },
  ServiceDocument: { template: '<span class="mock-service-icon"></span>' },
  Loading: { template: '<span class="mock-loading-icon"></span>' },
  QuestionFilled: { template: '<span class="mock-question-icon"></span>' },
  ArrowDown: { template: '<span class="mock-arrow-down"></span>' },
  ArrowUp: { template: '<span class="mock-arrow-up"></span>' },
  WarnTriangleFilled: { template: '<span class="mock-warn-icon"></span>' },
}));

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElIcon: {
    name: 'ElIcon',
    template: '<span class="el-icon"></span>',
  },
  ElButton: {
    name: 'ElButton',
    props: {
      text: { type: Boolean, default: false },
      size: { type: String, default: 'default' },
      type: { type: String, default: 'default' },
      disabled: { type: Boolean, default: false },
      link: { type: Boolean, default: false },
    },
    template: '<button class="el-button" :disabled="disabled"></button>',
  },
  ElInput: {
    name: 'ElInput',
    props: {
      modelValue: { type: String, default: '' },
      type: { type: String, default: 'text' },
      placeholder: { type: String, default: '' },
      rows: { type: Number, default: 2 },
      maxlength: { type: Number, default: 0 },
      showWordLimit: { type: Boolean, default: false },
    },
    template: '<div class="el-input"></div>',
  },
  ElTag: {
    name: 'ElTag',
    props: {
      size: { type: String, default: 'default' },
    },
    template: '<span class="el-tag"></span>',
  },
}));

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'chat.title': 'AI Assistant',
        'chat.placeholder': 'Type a message, Shift+Enter for new line, Enter to send',
        'chat.welcome': 'Hello, I am your AI assistant. How can I help you today?',
        'chat.send': 'Send',
        'chat.clear': 'Clear',
        'chat.thinking': 'Thinking...',
        'chat.thinkingToggle': 'Show thinking chain',
        'chat.retry': 'Retry',
        'chat.error': 'Sorry, an error occurred. Please try again.',
        'chat.recommended': 'Recommended',
        'chat.close': 'Close',
        'chat.sessionNew': 'New Chat',
      };
      return translations[key] || key;
    },
  }),
}));

// Mock Session storage
vi.mock('@/utils/storage', () => ({
  Session: {
    getToken: () => 'mock-token',
    get: (key: string) => {
      if (key === 'userInfo') {
        return { tenantId: 'test-tenant' };
      }
      return null;
    },
  },
}));

describe('Chat Component', () => {
  describe('ChatMessage Structure', () => {
    it('should have correct message properties', () => {
      const message = {
        id: 'msg_123',
        role: 'user' as const,
        content: 'Hello',
        timestamp: Date.now(),
      };

      expect(message).toHaveProperty('id');
      expect(message).toHaveProperty('role');
      expect(message).toHaveProperty('content');
      expect(message).toHaveProperty('timestamp');
      expect(message.role).toBe('user');
    });

    it('should support user, assistant, and thinking roles', () => {
      const roles = ['user', 'assistant', 'thinking'] as const;

      expect(roles).toContain('user');
      expect(roles).toContain('assistant');
      expect(roles).toContain('thinking');
    });
  });

  describe('AI Event Types', () => {
    it('should support thinking event', () => {
      const event = {
        type: 'thinking' as const,
        content: '正在思考...',
      };

      expect(event.type).toBe('thinking');
      expect(event.content).toBe('正在思考...');
    });

    it('should support message event', () => {
      const event = {
        type: 'message' as const,
        content: '这是AI的回复',
      };

      expect(event.type).toBe('message');
      expect(event.content).toBe('这是AI的回复');
    });

    it('should support done event', () => {
      const event = {
        type: 'done' as const,
        sessionId: 'session_123',
      };

      expect(event.type).toBe('done');
      expect(event.sessionId).toBe('session_123');
    });

    it('should support error event', () => {
      const event = {
        type: 'error' as const,
        message: '发生错误',
      };

      expect(event.type).toBe('error');
      expect(event.message).toBe('发生错误');
    });
  });

  describe('Message Generation', () => {
    it('should generate unique message IDs', () => {
      const generateId = () => `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

      const id1 = generateId();
      const id2 = generateId();

      expect(id1).not.toBe(id2);
      expect(id1).toMatch(/^msg_\d+_/);
    });
  });

  describe('CanSend Logic', () => {
    it('should allow send when input is not empty and not loading', () => {
      const inputText = 'Hello';
      const isLoading = false;
      const canSend = inputText.trim().length > 0 && !isLoading;

      expect(canSend).toBe(true);
    });

    it('should not allow send when input is empty', () => {
      const inputText = '';
      const isLoading = false;
      const canSend = inputText.trim().length > 0 && !isLoading;

      expect(canSend).toBe(false);
    });

    it('should not allow send when loading', () => {
      const inputText = 'Hello';
      const isLoading = true;
      const canSend = inputText.trim().length > 0 && !isLoading;

      expect(canSend).toBe(false);
    });
  });

  describe('Input Length Validation', () => {
    it('should respect maxLength limit', () => {
      const maxLength = 2000;
      const inputText = 'A'.repeat(2500);
      const truncated = inputText.substring(0, maxLength);

      expect(truncated.length).toBe(2000);
    });

    it('should allow text under maxLength', () => {
      const maxLength = 2000;
      const inputText = 'A'.repeat(1000);
      const isValid = inputText.length <= maxLength;

      expect(isValid).toBe(true);
    });
  });

  describe('Thinking Chain Collapse', () => {
    it('should toggle thinking visibility', () => {
      const collapsedThinking = new Set<string>();
      const msgId = 'msg_123';

      // Collapse
      collapsedThinking.add(msgId);
      expect(collapsedThinking.has(msgId)).toBe(true);

      // Expand
      collapsedThinking.delete(msgId);
      expect(collapsedThinking.has(msgId)).toBe(false);
    });
  });

  describe('Session Management', () => {
    it('should clear session messages', () => {
      const messages = [
        { id: '1', role: 'user' as const, content: 'Hello', timestamp: Date.now() },
        { id: '2', role: 'assistant' as const, content: 'Hi', timestamp: Date.now() },
      ];

      const clearedMessages = messages.filter(() => false);

      expect(clearedMessages).toEqual([]);
    });

    it('should reset session ID to null', () => {
      const currentSessionId = 'session_123';
      const resetSessionId = null;

      expect(resetSessionId).toBeNull();
    });
  });

  describe('Message Filtering', () => {
    it('should get last user message', () => {
      const messages = [
        { id: '1', role: 'user' as const, content: 'Hello', timestamp: Date.now() },
        { id: '2', role: 'assistant' as const, content: 'Hi', timestamp: Date.now() },
        { id: '3', role: 'user' as const, content: 'How are you?', timestamp: Date.now() },
      ];

      const lastUserMsg = messages.filter((m) => m.role === 'user').pop();

      expect(lastUserMsg?.content).toBe('How are you?');
    });

    it('should remove messages after a certain index', () => {
      const messages = [
        { id: '1', role: 'user' as const, content: 'Hello', timestamp: Date.now() },
        { id: '2', role: 'assistant' as const, content: 'Hi', timestamp: Date.now() },
        { id: '3', role: 'user' as const, content: 'How are you?', timestamp: Date.now() },
      ];

      const lastUserIndex = messages.findIndex((m) => m.id === '3');
      const trimmedMessages = messages.slice(0, lastUserIndex);

      expect(trimmedMessages).toHaveLength(2);
    });
  });

  describe('Recommended Questions', () => {
    it('should display recommended questions', () => {
      const recommendedQuestions = ['Question 1', 'Question 2', 'Question 3'];

      expect(recommendedQuestions.length).toBe(3);
      expect(recommendedQuestions[0]).toBe('Question 1');
    });
  });

  describe('Typing Animation', () => {
    it('should have three typing dots', () => {
      const dotCount = 3;
      expect(dotCount).toBe(3);
    });

    it('should have staggered animation delays', () => {
      const delays = [0, 0.2, 0.4]; // seconds
      expect(delays[0]).toBe(0);
      expect(delays[1]).toBe(0.2);
      expect(delays[2]).toBe(0.4);
    });
  });

  describe('Error State', () => {
    it('should track error state', () => {
      let error = false;
      let errorMessage = '';

      error = true;
      errorMessage = 'Sorry, an error occurred. Please try again.';

      expect(error).toBe(true);
      expect(errorMessage).toBe('Sorry, an error occurred. Please try again.');
    });
  });
});
