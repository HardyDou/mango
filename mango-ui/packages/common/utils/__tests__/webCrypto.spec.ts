import { describe, expect, it, vi } from 'vitest';
import {
  createWebCryptoRandomUUID,
  generateRfc4122UuidV4,
  installWebCryptoRandomUUIDCompatibility,
  type MangoWebCryptoLike,
} from '../webCrypto';

const UUID_V4_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u;

function deterministicRandomValues(array: Uint8Array) {
  array.forEach((_value, index) => {
    array[index] = index;
  });
  return array;
}

describe('Web Crypto randomUUID compatibility', () => {
  it('keeps and uses the native implementation when it exists', () => {
    const nativeRandomUUID = vi.fn(() => '00000000-0000-4000-8000-000000000000');
    const crypto: MangoWebCryptoLike = {
      getRandomValues: deterministicRandomValues,
      randomUUID: nativeRandomUUID,
    };

    expect(installWebCryptoRandomUUIDCompatibility({ crypto })).toEqual({ status: 'native' });
    expect(crypto.randomUUID).toBe(nativeRandomUUID);
    expect(createWebCryptoRandomUUID(crypto)).toBe('00000000-0000-4000-8000-000000000000');
    expect(nativeRandomUUID).toHaveBeenCalledOnce();
  });

  it('generates an RFC 4122 v4 UUID from getRandomValues', () => {
    const uuid = generateRfc4122UuidV4(deterministicRandomValues);

    expect(uuid).toBe('00010203-0405-4607-8809-0a0b0c0d0e0f');
    expect(uuid).toMatch(UUID_V4_PATTERN);
  });

  it('installs the fallback once and remains idempotent', () => {
    const crypto: MangoWebCryptoLike = { getRandomValues: deterministicRandomValues };

    expect(installWebCryptoRandomUUIDCompatibility({ crypto })).toEqual({ status: 'installed' });
    const installed = crypto.randomUUID;
    expect(installed?.()).toMatch(UUID_V4_PATTERN);
    expect(installWebCryptoRandomUUIDCompatibility({ crypto })).toEqual({ status: 'native' });
    expect(crypto.randomUUID).toBe(installed);
  });

  it('falls back to a dedicated Crypto prototype when the instance cannot be extended', () => {
    const cryptoPrototype: MangoWebCryptoLike = { getRandomValues: deterministicRandomValues };
    const crypto = Object.freeze(Object.create(cryptoPrototype)) as MangoWebCryptoLike;

    expect(installWebCryptoRandomUUIDCompatibility({ crypto })).toEqual({ status: 'installed' });
    expect(crypto.randomUUID?.()).toMatch(UUID_V4_PATTERN);
    expect(Object.hasOwn(crypto, 'randomUUID')).toBe(false);
    expect(Object.hasOwn(cryptoPrototype, 'randomUUID')).toBe(true);
  });

  it('does not install a weak implementation without Web Crypto', () => {
    const crypto: MangoWebCryptoLike = {};

    expect(installWebCryptoRandomUUIDCompatibility({})).toEqual({
      status: 'unavailable',
      reason: 'crypto-unavailable',
    });
    expect(installWebCryptoRandomUUIDCompatibility({ crypto })).toEqual({
      status: 'unavailable',
      reason: 'get-random-values-unavailable',
    });
    expect(crypto.randomUUID).toBeUndefined();
    expect(createWebCryptoRandomUUID(crypto)).toBeUndefined();
  });
});
