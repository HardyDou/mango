import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Sprint 4 Consumer',
}).mount();
