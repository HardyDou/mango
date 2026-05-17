import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { registerUnauthorizedHandler } from '@mango/common';
import { registerDefaultAdminPages } from '@mango/admin-pages';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import App from './App.vue';
import router from './router';

const app = createApp(App);

registerDefaultAdminPages();

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(ElementPlus);
app.use(createPinia());
app.use(router);
app.config.globalProperties.$t = (key: string) => ({
  'login.title': 'Mango 管理平台',
  'login.username.placeholder': '用户名',
  'login.password.placeholder': '密码',
  'login.btn': '登 录',
}[key] || key);

registerUnauthorizedHandler(async () => {
  await router.push('/login');
});

app.mount('#app');
