<template>
  <DemoDocLayout
    class="captcha-demo"
    title="验证码组件"
    subtitle="统一使用 CaptchaSelector 组件，按场景固定验证码类型或启用综合选择器。"
    content-box
    :toc-items="tocItems"
  >
    <section id="arithmetic" class="doc-section">
      <h2>算术验证码</h2>
      <p>适合登录、表单提交等轻量校验场景；验证成功后返回 captchaKey 和用户输入结果。</p>
      <div class="demo-block">
        <div class="demo-source">
          <CaptchaSelector
            :type="CaptchaType.ARITHMETIC"
            @success="handleSuccess"
            @refresh="handleRefresh"
          />
        </div>
        <div class="op-btns" @click="toggleCode('arithmetic')">
          <el-icon><component :is="codeVisible.arithmetic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.arithmetic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.arithmetic" :code="arithmeticCode" />
      </div>
    </section>

    <section id="block" class="doc-section">
      <h2>图片滑块验证码</h2>
      <p>使用后端返回的背景图和目标坐标，前端只负责拖动交互并提交 pointJson 校验。</p>
      <div class="demo-block">
        <div class="demo-source demo-panel-medium">
          <CaptchaSelector
            :type="CaptchaType.BLOCK_PUZZLE"
            @success="handleSuccess"
            @refresh="handleRefresh"
          />
        </div>
        <div class="op-btns" @click="toggleCode('block')">
          <el-icon><component :is="codeVisible.block ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.block ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.block" :code="blockCode" />
      </div>
    </section>

    <section id="sms" class="doc-section">
      <h2>短信验证码</h2>
      <p>适合手机号确认、登录二次校验等场景；发送接口返回 captchaKey，输入验证码后统一走 verify 接口。</p>
      <div class="demo-block">
        <div class="demo-source demo-panel-medium">
          <CaptchaSelector
            :type="CaptchaType.SMS"
            @success="handleSuccess"
          />
        </div>
        <div class="op-btns" @click="toggleCode('sms')">
          <el-icon><component :is="codeVisible.sms ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.sms ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.sms" :code="smsCode" />
      </div>
    </section>

    <section id="email" class="doc-section">
      <h2>邮件验证码</h2>
      <p>适合邮箱确认、找回密码等场景；组件内置邮箱格式校验、倒计时和验证码提交。</p>
      <div class="demo-block">
        <div class="demo-source demo-panel-medium">
          <CaptchaSelector
            :type="CaptchaType.EMAIL"
            @success="handleSuccess"
          />
        </div>
        <div class="op-btns" @click="toggleCode('email')">
          <el-icon><component :is="codeVisible.email ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.email ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.email" :code="emailCode" />
      </div>
    </section>

    <section id="selector" class="doc-section">
      <h2>综合选择器</h2>
      <p>不传 type 时展示组件内置的类型切换，用于调试或需要用户选择验证方式的场景。</p>
      <div class="demo-block">
        <div class="demo-source demo-panel-medium">
          <CaptchaSelector
            @success="handleSuccess"
            @refresh="handleRefresh"
          />
        </div>
        <div class="op-btns" @click="toggleCode('selector')">
          <el-icon><component :is="codeVisible.selector ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.selector ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.selector" :code="selectorCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="defaultValue" label="默认值" width="120" />
      </el-table>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>支持方法 / 事件</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="payload" label="参数" min-width="240" />
      </el-table>
    </section>

    <section id="response" class="doc-section api-section">
      <h2>返回字段</h2>
      <el-table :data="responseTable" size="small" border>
        <el-table-column prop="name" label="字段" width="180" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="example" label="示例" min-width="220" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="CaptchaDemo">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { CaptchaSelector, CaptchaType } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'arithmetic', label: '算术验证码' },
  { id: 'block', label: '图片滑块验证码' },
  { id: 'sms', label: '短信验证码' },
  { id: 'email', label: '邮件验证码' },
  { id: 'selector', label: '综合选择器' },
  { id: 'props', label: '支持属性' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'response', label: '返回字段' },
];

const codeVisible = ref<Record<string, boolean>>({
  arithmetic: false,
  block: false,
  sms: false,
  email: false,
  selector: false,
});

const arithmeticCode = `<CaptchaSelector
  :type="CaptchaType.ARITHMETIC"
  @success="handleSuccess"
  @refresh="handleRefresh"
/>`;

const blockCode = `<CaptchaSelector
  :type="CaptchaType.BLOCK_PUZZLE"
  @success="handleSuccess"
  @refresh="handleRefresh"
/>`;

const smsCode = `<CaptchaSelector
  :type="CaptchaType.SMS"
  @success="handleSuccess"
/>`;

const emailCode = `<CaptchaSelector
  :type="CaptchaType.EMAIL"
  @success="handleSuccess"
/>`;

const selectorCode = `<CaptchaSelector
  @success="handleSuccess"
  @refresh="handleRefresh"
/>`;

const propsTable = [
  { name: 'type', description: '固定验证码类型；不传时展示综合选择器', type: 'CaptchaType', defaultValue: '-' },
];

const eventsTable = [
  { name: 'success', description: '验证码通过后触发', payload: '(key: string, code?: string, type?: CaptchaType) => void' },
  { name: 'refresh', description: '刷新图形验证码后触发', payload: '() => void' },
  { name: 'refresh', description: '组件暴露方法，用于刷新当前验证码', payload: '() => void' },
];

const responseTable = [
  { name: 'key', description: '验证码键，提交业务接口或 verify 接口时使用', example: 'captcha-key' },
  { name: 'type', description: '验证码类型', example: 'ARITHMETIC / BLOCK_PUZZLE / SMS / EMAIL' },
  { name: 'image', description: '算术验证码图片', example: 'data:image/png;base64,...' },
  { name: 'backgroundImage', description: '图片滑块背景图', example: 'data:image/png;base64,...' },
  { name: 'x', description: '图片滑块目标 X 坐标，前端只用于展示目标位置', example: '128' },
  { name: 'expireTime', description: '过期时间，单位秒', example: '300' },
  { name: 'target', description: '短信或邮件验证码发送目标', example: '13800138000' },
];

function handleSuccess(key: string, code?: string, type?: CaptchaType) {
  ElMessage.success(`验证成功：${type ?? 'UNKNOWN'} / ${key}${code ? ` / ${code}` : ''}`);
}

function handleRefresh() {}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

</script>

<style scoped lang="scss">
@use './demo-page.scss';

.captcha-demo {
  :deep(.captcha-card),
  :deep(.captcha-form) {
    max-width: 420px;
  }
}
</style>
