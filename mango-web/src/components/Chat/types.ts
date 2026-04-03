/**
 * Chat Component Types - AI Dialogue
 */

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'thinking';
  content: string;
  timestamp: number;
}

export interface ChatSession {
  sessionId: string;
  messages: ChatMessage[];
}

export interface ChatProps {
  /** Default session ID */
  sessionId?: string;
  /** Welcome message shown on empty state */
  welcomeMessage?: string;
  /** Recommended questions */
  recommendedQuestions?: string[];
  /** Enable thinking chain display */
  enableThinking?: boolean;
  /** Maximum message length */
  maxLength?: number;
  /** Placeholder for input */
  placeholder?: string;
}

export interface ChatEmits {
  (e: 'session-change', sessionId: string): void;
  (e: 'message-send', content: string): void;
  (e: 'error', error: Error): void;
}

export interface ChatExpose {
  /** Open the chat window */
  open(): void;
  /** Close the chat window */
  close(): void;
  /** Toggle chat window visibility */
  toggle(): void;
  /** Clear current session */
  clearSession(): void;
  /** Get current session ID */
  getSessionId(): string | null;
}

export type ChatInstance = InstanceType<typeof import('./index.vue').default>;

/**
 * SSE Event types for AI chat
 */
export interface AIThinkingEvent {
  type: 'thinking';
  content: string;
}

export interface AIMessageEvent {
  type: 'message';
  content: string;
}

export interface AIDoneEvent {
  type: 'done';
  sessionId: string;
}

export interface AIErrorEvent {
  type: 'error';
  message: string;
}

export type AIEvent = AIThinkingEvent | AIMessageEvent | AIDoneEvent | AIErrorEvent;
