<template>
  <DemoDocLayout
    class="component-demo"
    title="前端组件库"
    subtitle="按高频业务场景整理的组件入口，详细用法请进入对应组件示例页查看。"
    content-box
    :toc-items="tocItems"
  >
    <section id="common" class="doc-section">
      <h2>高频表单组件</h2>
      <p>文件上传、验证码、组织选择、省市区选择是后台业务表单里使用频率最高的一组组件。</p>
      <div class="component-grid">
        <ComponentEntry title="MUpload 文件上传" desc="附件、图片、文档和业务附件清单。" path="/components/upload" />
        <ComponentEntry title="验证码组件" desc="算术、滑块、短信、邮件和综合选择器。" path="/components/captcha" />
        <ComponentEntry title="组织架构选择器" desc="单选、多选、禁用和组织 ID 回显。" path="/components/org-selector" />
        <ComponentEntry title="省市区选择器" desc="三级、四级和仅显示末级模式。" path="/components/china-area" />
      </div>
    </section>

    <section id="content" class="doc-section">
      <h2>内容编辑组件</h2>
      <p>富文本和代码编辑器用于公告、协议、脚本、SQL、JSON 配置等内容录入。</p>
      <div class="component-grid">
        <ComponentEntry title="富文本编辑器" desc="完整模式、简洁模式、只读和方法调用。" path="/components/editor" />
        <ComponentEntry title="代码编辑器" desc="语言模式、主题、只读和编辑器方法。" path="/components/code-editor" />
      </div>
    </section>

    <section id="data" class="doc-section">
      <h2>数据与权限</h2>
      <p>图表和权限指令用于数据展示、操作按钮可见性控制和常见后台管理页面。</p>
      <div class="component-grid">
        <ComponentEntry title="数据图表" desc="折线图、柱状图、饼图、loading 和主题。" path="/components/charts" />
        <ComponentEntry title="功能指令" desc="v-auth、v-auths、v-auth-all 权限显示控制。" path="/components/directive" />
      </div>
    </section>

    <section id="realtime" class="doc-section">
      <h2>实时通信</h2>
      <p>AI 对话、WebSocket 和 SSE 用于在线交互、消息推送和服务端通知。</p>
      <div class="component-grid">
        <ComponentEntry title="AI 对话组件" desc="浮动对话、推荐问题、会话 ID 和流式返回。" path="/components/chat" />
        <ComponentEntry title="WebSocket 客户端" desc="认证参数、心跳、重连和消息发送。" path="/components/websocket" />
        <ComponentEntry title="服务端推送 (SSE)" desc="服务端单向推送、重连和消息通知。" path="/components/sse" />
      </div>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="ComponentDemo">
import { defineComponent, h } from 'vue';
import { useRouter } from 'vue-router';
import DemoDocLayout from './DemoDocLayout.vue';

const router = useRouter();

const tocItems = [
  { id: 'common', label: '高频表单' },
  { id: 'content', label: '内容编辑' },
  { id: 'data', label: '数据与权限' },
  { id: 'realtime', label: '实时通信' },
];

const ComponentEntry = defineComponent({
  props: {
    title: { type: String, required: true },
    desc: { type: String, required: true },
    path: { type: String, required: true },
  },
  setup(props) {
    return () => h(
      'button',
      {
        class: 'component-entry',
        type: 'button',
        onClick: () => router.push(props.path),
      },
      [
        h('span', { class: 'entry-title' }, props.title),
        h('span', { class: 'entry-desc' }, props.desc),
      ],
    );
  },
});
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.component-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.component-entry {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 104px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 6px 16px rgb(31 45 61 / 8%);
    transform: translateY(-1px);
  }
}

.entry-title {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.entry-desc {
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.7;
}
</style>
