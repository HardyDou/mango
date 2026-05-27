import { createMangoAdminApp } from '@mango/admin-shell';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || 'Mango Admin',
}).mount();
