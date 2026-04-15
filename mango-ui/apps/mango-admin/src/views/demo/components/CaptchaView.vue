<template>
  <div class="captcha-demo">
    <el-card header="验证码组件演示">
      <el-alert
        title="验证码组件说明"
        type="info"
        :closable="false"
        description="支持多种验证码类型：算术验证码、滑块验证码、短信验证码、邮件验证码"
        style="margin-bottom: 20px"
      />
      <el-tabs v-model="activeTab">
        <el-tab-pane
          label="算术验证码"
          name="arithmetic"
        >
          <el-card header="算术验证码">
            <ArithmeticCaptcha
              ref="arithmeticRef"
              @success="handleSuccess('算术验证码', $event)"
              @refresh="handleRefresh('算术验证码')"
            />
          </el-card>
        </el-tab-pane>
        <el-tab-pane
          label="滑块验证码"
          name="blockPuzzle"
        >
          <el-card header="滑块验证码（图片）">
            <BlockPuzzleCaptcha
              ref="blockPuzzleRef"
              @success="handleSuccess('图片滑块验证码', $event)"
              @refresh="handleRefresh('图片滑块验证码')"
            />
          </el-card>
        </el-tab-pane>
        <el-tab-pane
          label="Canvas滑块"
          name="canvasSlider"
        >
          <el-card header="Canvas滑块验证码（纯前端）">
            <CanvasSliderCaptcha
              ref="canvasSliderRef"
              @success="handleSuccess('Canvas滑块验证码', $event)"
              @refresh="handleRefresh('Canvas滑块验证码')"
            />
          </el-card>
        </el-tab-pane>
        <el-tab-pane
          label="短信验证码"
          name="sms"
        >
          <el-card header="短信验证码">
            <SmsCaptcha
              ref="smsRef"
              @success="handleSuccess('短信验证码', $event)"
            />
          </el-card>
        </el-tab-pane>
        <el-tab-pane
          label="邮件验证码"
          name="email"
        >
          <el-card header="邮件验证码">
            <EmailCaptcha
              ref="emailRef"
              @success="handleSuccess('邮件验证码', $event)"
            />
          </el-card>
        </el-tab-pane>
        <el-tab-pane
          label="综合选择器"
          name="selector"
        >
          <el-card header="验证码选择器">
            <CaptchaSelector
              ref="selectorRef"
              @success="handleSelectorSuccess"
              @refresh="handleSelectorRefresh"
            />
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <el-card
        header="使用说明"
        style="margin-top: 20px"
      >
        <el-steps :active="0">
          <el-step
            title="选择类型"
            description="通过 Tab 或选择器切换验证码类型"
          />
          <el-step
            title="完成验证"
            description="根据不同类型完成验证操作"
          />
          <el-step
            title="获取 Key"
            description="验证成功后获取 captchaKey 用于后续流程"
          />
        </el-steps>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="CaptchaDemo">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  ArithmeticCaptcha,
  BlockPuzzleCaptcha,
  CanvasSliderCaptcha,
  SmsCaptcha,
  EmailCaptcha,
  CaptchaSelector,
} from '@mango/common';

const activeTab = ref('canvasSlider');

const arithmeticRef = ref<InstanceType<typeof ArithmeticCaptcha> | null>(null);
const blockPuzzleRef = ref<InstanceType<typeof BlockPuzzleCaptcha> | null>(null);
const canvasSliderRef = ref<InstanceType<typeof CanvasSliderCaptcha> | null>(null);
const smsRef = ref<InstanceType<typeof SmsCaptcha> | null>(null);
const emailRef = ref<InstanceType<typeof EmailCaptcha> | null>(null);
const selectorRef = ref<InstanceType<typeof CaptchaSelector> | null>(null);

const handleSuccess = (type: string, key: string) => {
  ElMessage.success(`${type} 验证成功，captchaKey: ${key}`);
};

const handleRefresh = (type: string) => {
  ElMessage.info(`${type} 已刷新`);
};

const handleSelectorSuccess = (key: string) => {
  ElMessage.success(`验证码验证成功，captchaKey: ${key}`);
};

const handleSelectorRefresh = () => {
  ElMessage.info('验证码已刷新');
};
</script>

<style scoped lang="scss">
.captcha-demo {
  padding: 20px;
}
</style>
