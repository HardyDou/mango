import { describe, expect, it, vi } from 'vitest';
import { configureMangoAdminShell } from '../config';
import { ensureFeatureRegistrars, resetFeatureRegistrarsForTest } from '../runtime/featureRegistrars';

describe('feature registrars', () => {
  it('runs configured feature registrars once before shell consumers read providers', async () => {
    resetFeatureRegistrarsForTest();
    const registrar = vi.fn();
    configureMangoAdminShell({ featureRegistrars: [registrar] });

    await ensureFeatureRegistrars();
    await ensureFeatureRegistrars();

    expect(registrar).toHaveBeenCalledTimes(1);
  });
});
