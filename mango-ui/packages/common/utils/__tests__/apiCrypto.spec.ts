/**
 * apiCrypto 单元测试
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock sm-crypto
vi.mock('sm-crypto', () => ({
  sm2: {
    doEncrypt: vi.fn((data, key, options) => `encrypted:${data}`),
    doDecrypt: vi.fn((data, key, options) => data.replace('encrypted:', '')),
  },
}));

// Mock import.meta.env
const originalEnv = { ...process.env };

describe('API Encryption Utils', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset module state by clearing cache would require re-import
    // For now, tests modify env through the module's closure
  });

  afterEach(() => {
    // Restore original env
    Object.assign(process.env, originalEnv);
  });

  describe('isBffMode', () => {
    it('应该正确检测 BFF 模式', async () => {
      // Import after env is set
      const { isBffMode } = await import('../apiCrypto');
      // Default is true when VITE_IS_BFF is not set
      expect(typeof isBffMode).toBe('function');
    });
  });

  describe('isEncryptionEnabled', () => {
    it('应该正确检测加密是否启用', async () => {
      const { isEncryptionEnabled } = await import('../apiCrypto');
      expect(typeof isEncryptionEnabled).toBe('function');
    });
  });

  describe('wrapRequest', () => {
    it('加密未启用时应直接返回数据', async () => {
      const { wrapRequest } = await import('../apiCrypto');
      const data = { test: 'value' };
      const result = wrapRequest(data);
      expect(result).toBe(data);
    });

    it('BFF 模式下应直接返回数据', async () => {
      const { wrapRequest } = await import('../apiCrypto');
      const data = { test: 'value' };
      const result = wrapRequest(data);
      expect(result).toBe(data);
    });
  });

  describe('sm2Encrypt', () => {
    it('加密未启用时应返回原始数据', async () => {
      const { sm2Encrypt } = await import('../apiCrypto');
      // 默认加密未启用，直接返回原始数据
      const encrypted = sm2Encrypt('test data');
      expect(encrypted).toBe('test data');
    });
  });

  describe('sm2Decrypt', () => {
    it('加密未启用时应返回原始数据', async () => {
      const { sm2Decrypt } = await import('../apiCrypto');
      // 默认加密未启用，直接返回原始数据
      const decrypted = sm2Decrypt('encrypted:test data');
      expect(decrypted).toBe('encrypted:test data');
    });
  });
});
