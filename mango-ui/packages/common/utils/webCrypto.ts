export interface MangoWebCryptoLike {
  getRandomValues?: (array: Uint8Array) => Uint8Array;
  randomUUID?: () => string;
}

export interface MangoWebCryptoScopeLike {
  crypto?: MangoWebCryptoLike;
}

export type WebCryptoRandomUUIDCompatibilityResult =
  | { status: 'native' }
  | { status: 'installed' }
  | { status: 'unavailable'; reason: 'crypto-unavailable' | 'get-random-values-unavailable' | 'installation-failed' };

/**
 * Generates an RFC 4122 version 4 UUID from a cryptographically secure byte source.
 */
export function generateRfc4122UuidV4(getRandomValues: (array: Uint8Array) => Uint8Array): string {
  const bytes = new Uint8Array(16);
  getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;

  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0'));
  return [
    hex.slice(0, 4).join(''),
    hex.slice(4, 6).join(''),
    hex.slice(6, 8).join(''),
    hex.slice(8, 10).join(''),
    hex.slice(10, 16).join(''),
  ].join('-');
}

/**
 * Uses native randomUUID when available and otherwise falls back to getRandomValues.
 * Returns undefined when the environment has no secure Web Crypto UUID source.
 */
export function createWebCryptoRandomUUID(webCrypto: MangoWebCryptoLike | undefined): string | undefined {
  if (!webCrypto) {
    return undefined;
  }
  if (typeof webCrypto.randomUUID === 'function') {
    return webCrypto.randomUUID.call(webCrypto);
  }
  if (typeof webCrypto.getRandomValues !== 'function') {
    return undefined;
  }
  return generateRfc4122UuidV4((bytes) => webCrypto.getRandomValues!(bytes));
}

/**
 * Installs a secure randomUUID compatibility method on the current Web Crypto object.
 * The operation is idempotent and never substitutes Math.random for Web Crypto.
 */
export function installWebCryptoRandomUUIDCompatibility(
  scope: MangoWebCryptoScopeLike = globalThis as unknown as MangoWebCryptoScopeLike,
): WebCryptoRandomUUIDCompatibilityResult {
  const webCrypto = scope.crypto;
  if (!webCrypto) {
    return { status: 'unavailable', reason: 'crypto-unavailable' };
  }
  if (typeof webCrypto.randomUUID === 'function') {
    return { status: 'native' };
  }
  if (typeof webCrypto.getRandomValues !== 'function') {
    return { status: 'unavailable', reason: 'get-random-values-unavailable' };
  }

  const compatibleRandomUUID = () => generateRfc4122UuidV4((bytes) => webCrypto.getRandomValues!(bytes));
  if (defineRandomUUID(webCrypto, compatibleRandomUUID)) {
    return { status: 'installed' };
  }

  const prototype = Object.getPrototypeOf(webCrypto) as object | null;
  if (prototype && prototype !== Object.prototype && defineRandomUUID(prototype, compatibleRandomUUID)) {
    return { status: 'installed' };
  }
  return { status: 'unavailable', reason: 'installation-failed' };
}

function defineRandomUUID(target: object, randomUUID: () => string) {
  try {
    Object.defineProperty(target, 'randomUUID', {
      configurable: true,
      enumerable: false,
      value: randomUUID,
      writable: true,
    });
    return true;
  } catch {
    return false;
  }
}
