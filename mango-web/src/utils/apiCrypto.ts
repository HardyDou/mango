/**
 * API Encryption Utilities
 *
 * Security Architecture:
 * - In BFF mode (production default): transparent pass-through, JSON goes directly
 * - In non-BFF mode (single deployment): SM2 encryption for session key transmission
 *
 * Frontend does NOT hold symmetric keys - encryption is done at BFF layer
 *
 * SM2 Cipher Mode: C1C2C3 (国密标准推荐模式)
 * - sm-crypto 默认使用 C1C2C3 模式
 * - 与后端 BouncyCastle/GmSSL 的 C1C2C3 输出格式一致，互通性已验证
 */

import { sm2 } from 'sm-crypto';

// Environment configurations
const ENCRYPTION_ENABLED = import.meta.env.VITE_API_ENC_ENABLED === 'true';
const IS_BFF_MODE = import.meta.env.VITE_IS_BFF !== 'false'; // default true
const SM2_PUBLIC_KEY = import.meta.env.VITE_SM2_PUBLIC_KEY || '';

/**
 * Check if BFF mode is enabled
 */
export function isBffMode(): boolean {
  return IS_BFF_MODE;
}

/**
 * Check if encryption is enabled
 */
export function isEncryptionEnabled(): boolean {
  return ENCRYPTION_ENABLED;
}

/**
 * Wrap request data based on configuration
 *
 * BFF mode: transparent pass-through
 * Non-BFF mode: SM2 encrypt session key
 */
export function wrapRequest(data: unknown): unknown {
  if (!ENCRYPTION_ENABLED) {
    return data;
  }

  if (IS_BFF_MODE) {
    // BFF mode: transparent pass-through, trust BFF layer
    return data;
  }

  // Non-BFF mode: SM2 encrypt session key
  const stringData = typeof data === 'string' ? data : JSON.stringify(data);

  if (!SM2_PUBLIC_KEY) {
    console.warn('[apiCrypto] SM2 public key not configured, sending plain data');
    return stringData;
  }

  // Use SM2 to encrypt the session key (data)
  // sm-crypto 默认使用 C1C2C3 模式，与后端一致
  try {
    const encrypted = sm2.doEncrypt(stringData, SM2_PUBLIC_KEY);
    return { data: encrypted };
  } catch (error) {
    console.error('[apiCrypto] Encryption failed, sending plain data:', error);
    return stringData;
  }
}

/**
 * SM2 encrypt data (for non-BFF mode)
 * @param data - data to encrypt
 * @returns encrypted data or original if encryption not enabled
 *
 * SM2 密文模式: C1C2C3 (sm-crypto 默认)
 */
export function sm2Encrypt(data: string): string {
  if (!ENCRYPTION_ENABLED || !SM2_PUBLIC_KEY) {
    return data;
  }

  // sm-crypto 默认使用 C1C2C3 模式
  return sm2.doEncrypt(data, SM2_PUBLIC_KEY);
}

/**
 * SM2 decrypt data (for non-BFF mode response)
 * @param encryptedData - encrypted data
 * @returns decrypted data or original if encryption not enabled
 *
 * SM2 密文模式: C1C2C3 (sm-crypto 默认)
 */
export function sm2Decrypt(encryptedData: string): string {
  if (!ENCRYPTION_ENABLED || !SM2_PUBLIC_KEY) {
    return encryptedData;
  }

  // sm-crypto 默认使用 C1C2C3 模式
  return sm2.doDecrypt(encryptedData, SM2_PUBLIC_KEY);
}
