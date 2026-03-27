import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersist from 'pinia-plugin-persistedstate';
import { ElMessage } from 'element-plus';
import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '@/theme/index.scss';

const app = createApp(App);

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

app.mount('#app');
