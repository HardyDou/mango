import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersist from 'pinia-plugin-persistedstate';
import { ElMessage } from 'element-plus';
import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import '@/theme/index.scss';
import { initThemeBeforeRender } from './utils/themeInit';

const app = createApp(App);

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue 错误:', err);
  console.error('组件:', instance);
  console.error('错误信息:', info);
  ElMessage.error('系统错误，请刷新页面');
};

app.use(createPinia().use(piniaPluginPersist));
app.use(router);
app.use(i18n);
app.use(ElementPlus);

// 在 Vue 首帧渲染前同步恢复 store 状态（布局/深色模式等），避免闪屏
initThemeBeforeRender();

app.mount('#app');
