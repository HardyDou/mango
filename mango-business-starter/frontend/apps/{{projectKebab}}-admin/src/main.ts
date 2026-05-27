import { createMangoAdminApp } from '@mango/admin-shell';
import { register{{modulePascal}}Pages } from '@{{projectKebab}}/{{moduleKebab}}';

register{{modulePascal}}Pages();

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || '{{projectPascal}} Admin',
}).mount();
