export interface MultipartUploadPolicy {
  multipartEnabled: boolean;
  multipartThreshold: number;
}

export function shouldUseMultipart(fileSize: number, policy: MultipartUploadPolicy): boolean {
  return policy.multipartEnabled && fileSize >= policy.multipartThreshold;
}

export async function sha256IfSupported(
  file: File,
  cryptoApi: Crypto | null | undefined = globalThis.crypto,
): Promise<string> {
  const subtle = cryptoApi?.subtle;
  if (!subtle) return '';
  try {
    const buffer = await file.arrayBuffer();
    const digest = await subtle.digest('SHA-256', buffer);
    return Array.from(new Uint8Array(digest))
      .map((item) => item.toString(16).padStart(2, '0'))
      .join('');
  } catch {
    return '';
  }
}
