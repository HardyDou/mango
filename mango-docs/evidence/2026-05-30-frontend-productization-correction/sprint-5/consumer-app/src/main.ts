import { createMangoAdminApp } from '@mango/admin';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';
import '@mango/admin/style.css';
import '@mango/workflow/style.css';
import '@mango/workflow-business-example/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Sprint 5 Consumer',
  features: ['workflow'],
  featureRegistrars: [
    registerMangoWorkflowAdminPages,
    registerMangoWorkflowBusinessExampleAdminPages,
  ],
  devCenter: {
    deployEnv: import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE,
  },
}).mount();
