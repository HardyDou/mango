import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Sprint 5 Consumer',
  devCenter: {
    deployEnv: import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE,
  },
}).mount();
