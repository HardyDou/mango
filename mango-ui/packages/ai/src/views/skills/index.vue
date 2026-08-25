<template>
  <MangoListPage class="mango-ai-config-page" data-page="ai.skills">
    <template #header><span>Skill 与工具</span></template>
    <el-card shadow="never" data-surface="ai.skills.workspace">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="Skill 列表" name="skills">
          <div class="mango-ai-config-page__toolbar">
            <span>Skill 定义与工具绑定</span
            ><el-button v-auth="'ai:skill:add'" type="primary" data-action="ai.skills.add" @click="openSkill()"
              >新增 Skill</el-button
            >
          </div>
          <el-table v-loading="loading" :data="skills" row-key="id" data-surface="ai.skills.table">
            <el-table-column prop="name" label="名称" min-width="160" /><el-table-column
              prop="code"
              label="编码"
              min-width="140"
            />
            <el-table-column label="工具数" width="100"
              ><template #default="{ row }">{{ row.toolIds.length }}</template></el-table-column
            >
            <el-table-column label="状态" width="100"
              ><template #default="{ row }"
                ><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template
              ></el-table-column
            >
            <el-table-column label="操作" width="170" fixed="right"
              ><template #default="{ row }"
                ><el-button v-auth="'ai:skill:edit'" link type="primary" @click="openSkill(row)">编辑</el-button
                ><el-button v-auth="'ai:skill:delete'" link type="danger" @click="removeSkill(row)"
                  >删除</el-button
                ></template
              ></el-table-column
            >
            <template #empty><el-empty description="暂无 Skill" /></template>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="工具列表" name="tools">
          <div class="mango-ai-config-page__toolbar">
            <span>MCP 和 HTTP 工具定义</span
            ><el-button v-auth="'ai:tool:add'" type="primary" data-action="ai.tools.add" @click="openTool()"
              >新增工具</el-button
            >
          </div>
          <el-table v-loading="loading" :data="tools" row-key="id" data-surface="ai.tools.table">
            <el-table-column prop="name" label="名称" min-width="160" /><el-table-column
              prop="code"
              label="编码"
              min-width="140"
            />
            <el-table-column prop="toolType" label="类型" width="110" /><el-table-column
              prop="endpoint"
              label="Endpoint"
              min-width="260"
              show-overflow-tooltip
            />
            <el-table-column label="状态" width="100"
              ><template #default="{ row }"
                ><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template
              ></el-table-column
            >
            <el-table-column label="操作" width="170" fixed="right"
              ><template #default="{ row }"
                ><el-button v-auth="'ai:tool:edit'" link type="primary" @click="openTool(row)">编辑</el-button
                ><el-button v-auth="'ai:tool:delete'" link type="danger" @click="removeTool(row)"
                  >删除</el-button
                ></template
              ></el-table-column
            >
            <template #empty><el-empty description="暂无工具" /></template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <MangoDialog
      v-model="skillDialog"
      :title="skillForm.id ? '编辑 Skill' : '新增 Skill'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="skillForm" label-width="110px"
        ><el-form-item label="编码" required
          ><el-input v-model="skillForm.code" maxlength="64" :disabled="Boolean(skillForm.id)" /></el-form-item
        ><el-form-item label="名称" required><el-input v-model="skillForm.name" maxlength="100" /></el-form-item
        ><el-form-item label="说明"><el-input v-model="skillForm.description" maxlength="500" /></el-form-item
        ><el-form-item label="指令" required
          ><el-input
            v-model="skillForm.instructions"
            type="textarea"
            :rows="7"
            maxlength="65535"
            show-word-limit /></el-form-item
        ><el-form-item label="工具"
          ><el-select v-model="skillForm.toolIds" multiple clearable placeholder="选择可调用工具" style="width: 100%"
            ><el-option
              v-for="tool in tools"
              :key="tool.id"
              :label="`${tool.name}（${tool.toolType}）`"
              :value="tool.id" /></el-select></el-form-item
        ><el-form-item label="启用"><el-switch v-model="skillForm.enabled" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="skillDialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveSkill">保存</el-button></template
      >
    </MangoDialog>
    <MangoDialog
      v-model="toolDialog"
      :title="toolForm.id ? '编辑工具' : '新增工具'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form :model="toolForm" label-width="120px"
        ><el-form-item label="编码" required
          ><el-input v-model="toolForm.code" maxlength="64" :disabled="Boolean(toolForm.id)" /></el-form-item
        ><el-form-item label="名称" required><el-input v-model="toolForm.name" maxlength="100" /></el-form-item
        ><el-form-item label="说明"><el-input v-model="toolForm.description" maxlength="500" /></el-form-item
        ><el-form-item label="类型" required
          ><el-select v-model="toolForm.toolType"
            ><el-option label="MCP" value="MCP" /><el-option label="HTTP" value="HTTP" /></el-select></el-form-item
        ><el-form-item label="Endpoint" required><el-input v-model="toolForm.endpoint" maxlength="1024" /></el-form-item
        ><el-form-item label="输入 Schema" required
          ><el-input v-model="toolForm.inputSchemaJson" type="textarea" :rows="5" /></el-form-item
        ><el-form-item label="输出 Schema" required
          ><el-input v-model="toolForm.outputSchemaJson" type="textarea" :rows="5" /></el-form-item
        ><el-form-item label="启用"><el-switch v-model="toolForm.enabled" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="toolDialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveTool">保存</el-button></template
      >
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import type { AiSkill, AiTool, AiToolType } from '@mango/ai-api';
import { MangoDialog, MangoListPage } from '@mango/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useAiConfigurationApi } from '../../composables/useAiConfigurationApi';
import { isDialogCancellation, isRequestAborted, requestErrorMessage } from '../../utils/requestError';

defineOptions({ name: 'AiSkillsView' });
const api = useAiConfigurationApi();
const activeTab = ref('skills');
const loading = ref(false);
const saving = ref(false);
const skillDialog = ref(false);
const toolDialog = ref(false);
let controller: AbortController | undefined;
const skills = ref<AiSkill[]>([]);
const tools = ref<AiTool[]>([]);
const skillForm = reactive<{
  id?: string;
  code: string;
  name: string;
  description: string;
  instructions: string;
  toolIds: string[];
  enabled: boolean;
}>({ code: '', name: '', description: '', instructions: '', toolIds: [], enabled: true });
const toolForm = reactive<{
  id?: string;
  code: string;
  name: string;
  description: string;
  toolType: AiToolType;
  endpoint: string;
  inputSchemaJson: string;
  outputSchemaJson: string;
  enabled: boolean;
}>({
  code: '',
  name: '',
  description: '',
  toolType: 'MCP',
  endpoint: '',
  inputSchemaJson: '{\n  "type": "object"\n}',
  outputSchemaJson: '{\n  "type": "object"\n}',
  enabled: true,
});
async function load() {
  controller?.abort();
  controller = new AbortController();
  loading.value = true;
  try {
    [skills.value, tools.value] = await Promise.all([api.skills(controller.signal), api.tools(controller.signal)]);
  } catch (error) {
    if (!isRequestAborted(error)) ElMessage.error(requestErrorMessage(error, '加载 Skill 与工具失败'));
  } finally {
    loading.value = false;
  }
}
function openSkill(item?: AiSkill) {
  Object.assign(
    skillForm,
    item
      ? { ...item, toolIds: [...item.toolIds] }
      : { id: undefined, code: '', name: '', description: '', instructions: '', toolIds: [], enabled: true },
  );
  skillDialog.value = true;
}
function openTool(item?: AiTool) {
  Object.assign(
    toolForm,
    item
      ? { ...item }
      : {
          id: undefined,
          code: '',
          name: '',
          description: '',
          toolType: 'MCP',
          endpoint: '',
          inputSchemaJson: '{\n  "type": "object"\n}',
          outputSchemaJson: '{\n  "type": "object"\n}',
          enabled: true,
        },
  );
  toolDialog.value = true;
}
function validJson(value: string) {
  try {
    const parsed: unknown = JSON.parse(value);
    return Boolean(parsed && typeof parsed === 'object' && !Array.isArray(parsed));
  } catch {
    return false;
  }
}
async function saveSkill() {
  if (!skillForm.code.trim() || !skillForm.name.trim() || !skillForm.instructions.trim())
    return ElMessage.error('请完整填写 Skill 信息');
  saving.value = true;
  try {
    const command = {
      code: skillForm.code,
      name: skillForm.name,
      description: skillForm.description,
      instructions: skillForm.instructions,
      toolIds: skillForm.toolIds,
      enabled: skillForm.enabled,
    };
    if (skillForm.id) await api.updateSkill({ id: skillForm.id, ...command });
    else await api.createSkill(command);
    skillDialog.value = false;
    ElMessage.success('Skill 已保存');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存 Skill 失败'));
  } finally {
    saving.value = false;
  }
}
async function saveTool() {
  if (
    !toolForm.code.trim() ||
    !toolForm.name.trim() ||
    !toolForm.endpoint.trim() ||
    !validJson(toolForm.inputSchemaJson) ||
    !validJson(toolForm.outputSchemaJson)
  )
    return ElMessage.error('请填写完整工具信息并保证 Schema 为 JSON 对象');
  saving.value = true;
  try {
    const command = {
      code: toolForm.code,
      name: toolForm.name,
      description: toolForm.description,
      toolType: toolForm.toolType,
      endpoint: toolForm.endpoint,
      inputSchemaJson: toolForm.inputSchemaJson,
      outputSchemaJson: toolForm.outputSchemaJson,
      enabled: toolForm.enabled,
    };
    if (toolForm.id) await api.updateTool({ id: toolForm.id, ...command });
    else await api.createTool(command);
    toolDialog.value = false;
    ElMessage.success('工具已保存');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存工具失败'));
  } finally {
    saving.value = false;
  }
}
async function removeSkill(item: AiSkill) {
  try {
    await ElMessageBox.confirm(`确认删除 Skill“${item.name}”？`, '删除 Skill', { type: 'warning' });
    await api.deleteSkill(item.id);
    ElMessage.success('Skill 已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除 Skill 失败'));
  }
}
async function removeTool(item: AiTool) {
  try {
    await ElMessageBox.confirm(`确认删除工具“${item.name}”？`, '删除工具', { type: 'warning' });
    await api.deleteTool(item.id);
    ElMessage.success('工具已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除工具失败'));
  }
}
watch(activeTab, () => {
  void load();
});
onMounted(() => {
  void load();
});
onBeforeUnmount(() => controller?.abort());
</script>
