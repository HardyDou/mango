import { sha256IfSupported, shouldUseMultipart } from '../uploadSupport';

describe('multipart upload support', () => {
  it('honors the runtime switch and threshold', () => {
    expect(shouldUseMultipart(20, { multipartEnabled: true, multipartThreshold: 20 })).toBe(true);
    expect(shouldUseMultipart(19, { multipartEnabled: true, multipartThreshold: 20 })).toBe(false);
    expect(shouldUseMultipart(100, { multipartEnabled: false, multipartThreshold: 20 })).toBe(false);
  });

  it('skips the client hash when Web Crypto is unavailable', async () => {
    const file = new File(['large-file'], 'large.bin');

    await expect(sha256IfSupported(file, null)).resolves.toBe('');
  });

  it('falls back when SubtleCrypto rejects the digest operation', async () => {
    const file = new File(['large-file'], 'large.bin');
    const cryptoApi = {
      subtle: {
        digest: vi.fn().mockRejectedValue(new DOMException('Not allowed', 'SecurityError')),
      },
    } as unknown as Crypto;

    await expect(sha256IfSupported(file, cryptoApi)).resolves.toBe('');
  });
});
