import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';
import { Session } from '@mango/common';
import { createMangoHttpClient } from '@mango/http-client';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { register{{modulePascal}}Pages } from '@{{projectKebab}}/{{moduleKebab}}';
import '@{{projectKebab}}/{{moduleKebab}}/style.css';

const apiBaseUrl = import.meta.env.VITE_MANGO_API_BASE_URL || '/api';
const httpClient = createMangoHttpClient({
  baseUrl: apiBaseUrl,
  getAccessToken: () => Session.getToken(),
  getTenantId: () => Session.get('userInfo')?.tenantId ?? Session.get('tenantId'),
  onUnauthorized: () => {
    Session.clearSession();
    window.location.hash = '/login';
  },
});

register{{modulePascal}}Pages();

const adminApp = createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl,
  title: import.meta.env.VITE_APP_TITLE || '{{projectPascal}} Admin',
});

adminApp.app.provide(MANGO_HTTP_CLIENT_KEY, httpClient);
adminApp.mount();
