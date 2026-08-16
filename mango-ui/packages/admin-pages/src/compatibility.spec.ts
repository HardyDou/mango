import { describe, expect, it } from 'vitest';

import { registerModulePages } from '@mango/admin-extension/core';
import { getPageLoader as getCompatibilityPageLoader } from './core';

describe('@mango/admin-pages compatibility exports', () => {
  it('shares the FE1 page registry with @mango/admin-extension', () => {
    const loader = async () => ({ name: 'CompatibilityPage' });

    registerModulePages({
      moduleCode: 'compatibility-module',
      pages: {
        'compatibility/index': loader,
      },
    });

    expect(getCompatibilityPageLoader('compatibility-module', 'compatibility/index')).toBe(loader);
  });
});
