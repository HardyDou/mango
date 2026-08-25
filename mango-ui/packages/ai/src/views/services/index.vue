<template>
  <MangoListPage class="mango-ai-config-page" data-page="ai.services">
    <template #header><span>AI 服务</span></template>
    <el-card shadow="never" data-surface="ai.services.list">
      <template #header
        ><div class="mango-ai-config-page__header">
          <div>
            <strong>业务服务定义</strong><small>绑定 Prompt、Skill 和输入输出 Schema，供后续业务入口调用</small>
          </div>
          <el-button v-auth="'ai:service:add'" type="primary" data-action="ai.services.add" @click="openService()"
            >新增服务</el-button
          >
        </div></template
      >
      <el-alert v-if="errorMessageText" :title="errorMessageText" type="error" :closable="false" show-icon />
      <el-table v-else v-loading="loading" :data="services" row-key="id" data-surface="ai.services.table">
        <el-table-column prop="name" label="名称" min-width="160" /><el-table-column
          prop="code"
          label="编码"
          min-width="140"
        /><el-table-column label="服务类型" width="140"
          ><template #default="{ row }">{{ serviceTypeLabel(row.serviceType) }}</template></el-table-column
        ><el-table-column prop="capability" label="模型能力" width="140" />
        <el-table-column label="绑定" min-width="210"
          ><template #default="{ row }"
            ><span>{{ row.promptName || '未绑定 Prompt' }}</span
            ><small v-if="row.skillName"> · {{ row.skillName }}</small></template
          ></el-table-column
        >
        <el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="操作" width="250" fixed="right"
          ><template #default="{ row }"
            ><el-button
              v-auth="'ai:service:invoke'"
              link
              type="primary"
              :disabled="!row.enabled"
              :data-record-key="row.code"
              data-action="ai.services.run"
              @click="openRun(row)"
              >运行</el-button
            ><el-button v-auth="'ai:service:edit'" link type="primary" @click="openService(row)">编辑</el-button
            ><el-button v-auth="'ai:service:delete'" link type="danger" @click="removeService(row)"
              >删除</el-button
            ></template
          ></el-table-column
        >
        <template #empty><el-empty description="暂无 AI 服务定义" /></template>
      </el-table>
    </el-card>

    <MangoDialog
      v-model="dialog"
      :title="form.id ? '编辑 AI 服务' : '新增 AI 服务'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="120px"
        ><el-form-item label="编码" required
          ><el-input v-model="form.code" maxlength="64" :disabled="Boolean(form.id)" /></el-form-item
        ><el-form-item label="名称" required><el-input v-model="form.name" maxlength="100" /></el-form-item
        ><el-form-item label="说明"><el-input v-model="form.description" maxlength="500" /></el-form-item
        ><el-form-item label="服务类型" required
          ><el-select v-model="form.serviceType"
            ><el-option
              v-for="item in serviceTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value" /></el-select></el-form-item
        ><el-form-item label="模型能力"
          ><el-select v-model="form.capability" clearable placeholder="选择路由能力"
            ><el-option
              v-for="item in capabilities"
              :key="item.value"
              :label="item.label"
              :value="item.value" /></el-select></el-form-item
        ><el-form-item label="Prompt"
          ><el-select v-model="form.promptId" clearable placeholder="选择 Prompt" style="width: 100%"
            ><el-option
              v-for="item in prompts"
              :key="item.id"
              :label="`${item.name}（${item.code}）`"
              :value="item.id" /></el-select></el-form-item
        ><el-form-item label="Skill"
          ><el-select v-model="form.skillId" clearable placeholder="选择 Skill" style="width: 100%"
            ><el-option
              v-for="item in skills"
              :key="item.id"
              :label="`${item.name}（${item.code}）`"
              :value="item.id" /></el-select></el-form-item
        ><el-row :gutter="16"
          ><el-col :span="12"
            ><el-form-item label="输入 Schema" required
              ><el-input v-model="form.inputSchemaJson" type="textarea" :rows="6" /></el-form-item></el-col
          ><el-col :span="12"
            ><el-form-item label="输出 Schema" required
              ><el-input v-model="form.outputSchemaJson" type="textarea" :rows="6" /></el-form-item></el-col></el-row
        ><el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveService">保存</el-button></template
      >
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import type { AiCapability, AiPrompt, AiService, AiServiceType, AiSkill } from '@mango/ai-api';
import { MangoDialog, MangoListPage } from '@mango/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAiConfigurationApi } from '../../composables/useAiConfigurationApi';
import { isDialogCancellation, isRequestAborted, requestErrorMessage } from '../../utils/requestError';

defineOptions({ name: 'AiServicesView' });
const api = useAiConfigurationApi();
const router = useRouter();
const services = ref<AiService[]>([]);
const prompts = ref<AiPrompt[]>([]);
const skills = ref<AiSkill[]>([]);
const loading = ref(false);
const saving = ref(false);
const dialog = ref(false);
const errorMessageText = ref('');
let controller: AbortController | undefined;
const capabilities: Array<{ value: AiCapability; label: string }> = [
  { value: 'CHAT', label: '聊天' },
  { value: 'EMBEDDING', label: '向量' },
  { value: 'RERANK', label: '排序' },
  { value: 'IMAGE_GENERATION', label: '图片生成' },
  { value: 'SPEECH_TO_TEXT', label: '语音识别' },
  { value: 'TEXT_TO_SPEECH', label: '语音合成' },
  { value: 'VIDEO_GENERATION', label: '视频生成' },
];
const serviceTypes: Array<{ value: AiServiceType; label: string }> = [
  { value: 'CHAT', label: '对话' },
  { value: 'EXTRACTION', label: '信息抽取' },
  { value: 'CLASSIFICATION', label: '文本分类' },
];
const form = reactive<{
  id?: string;
  code: string;
  name: string;
  description: string;
  serviceType: AiServiceType;
  capability?: AiCapability;
  promptId?: string;
  skillId?: string;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
}>({
  code: '',
  name: '',
  description: '',
  serviceType: 'EXTRACTION',
  capability: 'CHAT',
  inputSchemaJson: '{\n  "type": "object"\n}',
  outputSchemaJson: '{\n  "type": "object"\n}',
  enabled: true,
});
async function load() {
  controller?.abort();
  controller = new AbortController();
  loading.value = true;
  errorMessageText.value = '';
  try {
    [services.value, prompts.value, skills.value] = await Promise.all([
      api.services(controller.signal),
      api.prompts(controller.signal),
      api.skills(controller.signal),
    ]);
  } catch (error) {
    if (!isRequestAborted(error)) errorMessageText.value = requestErrorMessage(error, '加载 AI 服务失败');
  } finally {
    loading.value = false;
  }
}
function openService(item?: AiService) {
  Object.assign(
    form,
    item
      ? { ...item, inputSchemaJson: item.inputSchemaJson, outputSchemaJson: item.outputSchemaJson }
      : {
          id: undefined,
          code: '',
          name: '',
          description: '',
          serviceType: 'EXTRACTION',
          capability: 'CHAT',
          promptId: undefined,
          skillId: undefined,
          inputSchemaJson: '{\n  "type": "object"\n}',
          outputSchemaJson: '{\n  "type": "object"\n}',
          enabled: true,
        },
  );
  dialog.value = true;
}
async function openRun(item: AiService) {
  await router.push({ path: '/ai/services/run', query: { serviceCode: item.code } });
}
function serviceTypeLabel(value: AiServiceType) {
  return serviceTypes.find((item) => item.value === value)?.label || value;
}
function validJson(value: string) {
  try {
    const parsed: unknown = JSON.parse(value);
    return Boolean(parsed && typeof parsed === 'object' && !Array.isArray(parsed));
  } catch {
    return false;
  }
}
async function saveService() {
  if (!form.code.trim() || !form.name.trim() || !validJson(form.inputSchemaJson) || !validJson(form.outputSchemaJson))
    return ElMessage.error('请完整填写服务信息并保证 Schema 为 JSON 对象');
  saving.value = true;
  try {
    const command = {
      code: form.code,
      name: form.name,
      description: form.description,
      serviceType: form.serviceType,
      capability: form.capability,
      promptId: form.promptId,
      skillId: form.skillId,
      inputSchemaJson: form.inputSchemaJson,
      outputSchemaJson: form.outputSchemaJson,
      enabled: form.enabled,
    };
    if (form.id) await api.updateService({ id: form.id, ...command });
    else await api.createService(command);
    dialog.value = false;
    ElMessage.success('AI 服务已保存');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存 AI 服务失败'));
  } finally {
    saving.value = false;
  }
}
async function removeService(item: AiService) {
  try {
    await ElMessageBox.confirm(`确认删除 AI 服务“${item.name}”？`, '删除 AI 服务', { type: 'warning' });
    await api.deleteService(item.id);
    ElMessage.success('AI 服务已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除 AI 服务失败'));
  }
}
onMounted(() => {
  void load();
});
onBeforeUnmount(() => controller?.abort());
</script>
