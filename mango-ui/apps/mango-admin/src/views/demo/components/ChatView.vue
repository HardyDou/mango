<template>
  <div class="chat-view-container">
    <h1>AI 对话组件</h1>
    <p class="subtitle">
      基于 SSE 的 AI 对话组件，支持思维链展示、会话管理、推荐问题
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>AI 对话演示</span>
          <el-button
            type="primary"
            size="small"
            @click="handleOpenChat"
          >
            打开对话窗口
          </el-button>
        </div>
      </template>

      <div class="chat-preview">
        <div class="preview-tip">
          <el-icon><ChatDotRound /></el-icon>
          <span>点击"打开对话窗口"按钮启动 Chat 组件 Floating 对话窗口</span>
        </div>

        <el-divider>组件属性</el-divider>

        <el-form
          label-width="120px"
          style="max-width: 600px"
        >
          <el-form-item label="欢迎消息">
            <el-input
              v-model="welcomeMessage"
              placeholder="自定义欢迎消息"
            />
          </el-form-item>

          <el-form-item label="启用思维链">
            <el-switch v-model="enableThinking" />
          </el-form-item>

          <el-form-item label="最大输入长度">
            <el-input-number
              v-model="maxLength"
              :min="100"
              :max="5000"
              :step="100"
            />
          </el-form-item>

          <el-form-item label="会话 ID">
            <el-input
              v-model="sessionId"
              placeholder="指定会话 ID"
              clearable
            />
          </el-form-item>

          <el-form-item label="推荐问题">
            <el-select
              v-model="recommendedQuestionsValue"
              multiple
              placeholder="选择推荐问题"
              style="width: 100%"
            >
              <el-option
                label="你好"
                value="hello"
              />
              <el-option
                label="你能做什么？"
                value="help"
              />
              <el-option
                label="介绍一下自己"
                value="introduce"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleApply"
            >
              应用配置
            </el-button>
            <el-button @click="handleReset">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>组件属性</span>
      </template>
      <el-table :data="propsTableData">
        <el-table-column
          prop="name"
          label="属性名"
          width="150"
        />
        <el-table-column
          prop="type"
          label="类型"
          width="150"
        />
        <el-table-column
          prop="default"
          label="默认值"
          width="120"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>组件事件</span>
      </template>
      <el-table :data="eventsTableData">
        <el-table-column
          prop="name"
          label="事件名"
          width="150"
        />
        <el-table-column
          prop="params"
          label="参数"
          width="200"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>暴露方法</span>
      </template>
      <el-table :data="exposeTableData">
        <el-table-column
          prop="name"
          label="方法名"
          width="150"
        />
        <el-table-column
          prop="params"
          label="参数"
          width="200"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>

    <!-- Chat 组件 -->
    <Chat
      ref="chatRef"
      :welcome-message="welcomeMessage"
      :enable-thinking="enableThinking"
      :max-length="maxLength"
      :session-id="sessionId"
      :recommended-questions="recommendedQuestions"
      @message-send="handleMessageSend"
      @session-change="handleSessionChange"
      @error="handleChatError"
    />
  </div>
</template>

<script setup lang="ts" name="ChatView">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound } from '@element-plus/icons-vue';
import { Chat } from '@mango/common';

const chatRef = ref<InstanceType<typeof Chat>>();
const welcomeMessage = ref('chat.welcome');
const enableThinking = ref(true);
const maxLength = ref(2000);
const sessionId = ref('');
const recommendedQuestionsValue = ref<string[]>(['hello', 'help']);

const recommendedQuestions = ['你好', '你能做什么？', '介绍一下自己'];

const propsTableData = [
  { name: 'welcomeMessage', type: 'string', default: 'chat.welcome', description: '欢迎消息文本或 i18n key' },
  { name: 'sessionId', type: 'string', default: "''", description: '初始会话 ID' },
  { name: 'enableThinking', type: 'boolean', default: 'true', description: '是否启用思维链展示' },
  { name: 'maxLength', type: 'number', default: '2000', description: '最大输入长度' },
  { name: 'placeholder', type: 'string', default: 'chat.placeholder', description: '输入框占位文本' },
  { name: 'recommendedQuestions', type: 'string[]', default: '[]', description: '推荐问题列表' },
];

const eventsTableData = [
  { name: 'message-send', params: 'string', description: '用户发送消息时触发' },
  { name: 'session-change', params: 'string', description: '会话 ID 变化时触发' },
  { name: 'error', params: 'Error', description: '发生错误时触发' },
];

const exposeTableData = [
  { name: 'open', params: '-', description: '打开对话窗口' },
  { name: 'close', params: '-', description: '关闭对话窗口' },
  { name: 'toggle', params: '-', description: '切换对话窗口状态' },
  { name: 'clearSession', params: '-', description: '清空当前会话' },
  { name: 'getSessionId', params: '-', description: '获取当前会话 ID' },
];

function handleOpenChat() {
  chatRef.value?.open();
}

function handleApply() {
  ElMessage.success('配置已应用');
}

function handleReset() {
  welcomeMessage.value = 'chat.welcome';
  enableThinking.value = true;
  maxLength.value = 2000;
  sessionId.value = '';
  recommendedQuestionsValue.value = ['hello', 'help'];
}

function handleMessageSend(message: string) {
  console.log('User message:', message);
}

function handleSessionChange(newSessionId: string) {
  console.log('Session changed:', newSessionId);
}

function handleChatError(err: Error) {
  console.error('Chat error:', err);
}
</script>

<style scoped lang="scss">
.chat-view-container {
  padding: 20px;

  h1 {
    margin-bottom: 8px;
    font-size: 24px;
    font-weight: 600;
  }

  .subtitle {
    margin-bottom: 20px;
    color: #909399;
  }

  .demo-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .chat-preview {
      .preview-tip {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 16px;
        background: #f5f7fa;
        border-radius: 4px;
        color: #606266;
        font-size: 14px;

        .el-icon {
          font-size: 20px;
          color: #409eff;
        }
      }
    }
  }
}
</style>
