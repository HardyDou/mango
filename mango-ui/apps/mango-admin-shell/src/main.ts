import { createApp } from 'vue';
import { registerUnauthorizedHandler } from '@mango/common/utils/request';
import { Session } from '@mango/common/utils/storage';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import App from './App.vue';
import router from './router';
import { installShellApp } from './appBootstrap';
import { onShellRuntimeUnauthorized } from './runtime/runtimeHost';

const app = createApp(App);

installShellApp(app);
app.use(router);

async function redirectToLogin() {
  Session.clearSession();
  await router.push('/login');
}

registerUnauthorizedHandler(redirectToLogin);
onShellRuntimeUnauthorized(redirectToLogin);

app.mount('#app');
