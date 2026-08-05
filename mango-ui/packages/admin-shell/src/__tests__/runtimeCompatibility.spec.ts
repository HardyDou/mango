import { beforeEach, describe, expect, it, vi } from 'vitest';

const { installWebCryptoRandomUUIDCompatibility } = vi.hoisted(() => ({
  installWebCryptoRandomUUIDCompatibility: vi.fn(),
}));

vi.mock('@mango/common/utils/webCrypto', () => ({
  installWebCryptoRandomUUIDCompatibility,
}));

import { configureMangoAdminShell } from '../config';

describe('admin shell runtime compatibility', () => {
  beforeEach(() => {
    installWebCryptoRandomUUIDCompatibility.mockClear();
  });

  it('installs Web Crypto UUID compatibility during framework configuration', () => {
    configureMangoAdminShell({});

    expect(installWebCryptoRandomUUIDCompatibility).toHaveBeenCalledOnce();
  });
});
