import { describe, expect, it, vi } from 'vitest';
import { configureMangoAdminShell } from '../config';
import { ensureFeatureRegistrars, resetFeatureRegistrarsForTest } from '../runtime/featureRegistrars';
import { getMangoAdminHomeWidgets, registerMangoAdminHomeWidgets } from '../runtime/homeWidgets';

describe('feature registrars', () => {
  it('runs configured feature registrars once before shell consumers read providers', async () => {
    resetFeatureRegistrarsForTest();
    const registrar = vi.fn();
    configureMangoAdminShell({ featureRegistrars: [registrar], widgets: [] });

    await ensureFeatureRegistrars();
    await ensureFeatureRegistrars();

    expect(registrar).toHaveBeenCalledTimes(1);
  });

  it('collects home widgets returned by feature registrars once', async () => {
    resetFeatureRegistrarsForTest();
    const widget = {
      type: 'business.demo.summary',
      title: '业务概览',
      groupName: '工作台',
      component: {},
    };
    const registrar = vi.fn(() => ({
      businessDomainCode: 'DEMO_DOMAIN',
      businessDomainName: '演示业务',
      widgets: [widget],
    }));
    configureMangoAdminShell({ featureRegistrars: [registrar], widgets: [] });

    await ensureFeatureRegistrars();
    await ensureFeatureRegistrars();

    expect(registrar).toHaveBeenCalledTimes(1);
    expect(getMangoAdminHomeWidgets()).toHaveLength(1);
    expect(getMangoAdminHomeWidgets()[0]?.type).toBe('business.demo.summary');
    expect(getMangoAdminHomeWidgets()[0]?.businessDomainCode).toBe('DEMO_DOMAIN');
    expect(getMangoAdminHomeWidgets()[0]?.domainCode).toBe('DEMO_DOMAIN');
    expect(getMangoAdminHomeWidgets()[0]?.moduleCode).toBe('DEMO_DOMAIN');
    expect(getMangoAdminHomeWidgets()[0]?.groupName).toBe('工作台');
    expect(getMangoAdminHomeWidgets()[0]?.category).toBe('演示业务');
  });

  it('dedupes directly configured and manually registered home widgets by type', async () => {
    resetFeatureRegistrarsForTest();
    const firstWidget = {
      type: 'business.demo.duplicate',
      title: '业务组件',
      component: {},
    };
    const ignoredWidget = {
      type: 'business.demo.duplicate',
      title: '重复业务组件',
      component: {},
    };
    configureMangoAdminShell({ featureRegistrars: [], widgets: [firstWidget] });

    await ensureFeatureRegistrars();
    registerMangoAdminHomeWidgets([ignoredWidget]);

    expect(getMangoAdminHomeWidgets()).toHaveLength(1);
    expect(getMangoAdminHomeWidgets()[0]?.title).toBe('业务组件');
  });
});
