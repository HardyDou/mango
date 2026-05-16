<template>
  <div class="workflow-page">
    <div v-if="designerMode" class="workflow-builder">
      <div class="builder-header">
        <el-button class="builder-back" text @click="closeDesigner">
          返回列表
        </el-button>
        <div class="builder-steps" role="navigation" aria-label="工作流设计步骤">
          <button
            v-for="(step, index) in designerSteps"
            :key="step.key"
            class="builder-step"
            :class="{ active: definitionStep === index, done: definitionStep > index }"
            type="button"
            @click="goDefinitionStep(index)"
          >
            <span class="builder-step-index">{{ index + 1 }}</span>
            <span class="builder-step-text">
              <strong>{{ step.title }}</strong>
              <em>{{ step.description }}</em>
            </span>
          </button>
        </div>
        <div class="builder-actions">
          <el-button @click="saveDefinitionDraft">保存草稿</el-button>
          <el-button :loading="publishing" type="primary" @click="publishDefinition">发布流程</el-button>
        </div>
      </div>

      <div class="builder-content">
        <section v-show="definitionStep === 0" class="builder-pane basic-pane">
          <el-form
            ref="definitionFormRef"
            :model="definitionForm"
            :rules="definitionRules"
            class="step-form builder-form"
            label-position="top"
          >
            <el-form-item class="basic-field full workflow-icon-item" label="流程图标">
              <div class="workflow-icon-field">
                <div class="workflow-icon-uploader" :class="{ empty: !workflowIconPreviewUrl(definitionForm.icon), filled: Boolean(workflowIconPreviewUrl(definitionForm.icon)) }">
                  <ImageUpload v-model="definitionForm.icon" :limit="1" :multiple="false" class="workflow-icon-upload-control" />
                  <div v-if="isWorkflowPresetIcon(definitionForm.icon)" class="workflow-icon-overlay workflow-icon-legacy-preview">
                    <el-icon><component :is="workflowIconComponent(definitionForm.icon)" /></el-icon>
                    <span>{{ workflowIconLabel(definitionForm.icon) }}</span>
                  </div>
                  <div v-else-if="!definitionForm.icon" class="workflow-icon-overlay workflow-icon-empty-state">
                    <el-icon><PictureFilled /></el-icon>
                    <span>图标</span>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-form-item class="basic-field full" prop="definitionKey">
              <template #label>
                <span class="field-label-with-help">
                  流程编码
                  <el-tooltip content="流程编码对应 Flowable process id，发布后保持稳定，不建议修改。" placement="top">
                    <el-icon><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <el-input v-model="definitionForm.definitionKey" :disabled="Boolean(definitionForm.id)" placeholder="如 guarantee_approve" />
            </el-form-item>
            <el-form-item class="basic-field full" label="流程名称" prop="definitionName">
              <el-input v-model="definitionForm.definitionName" placeholder="请输入流程名称" />
            </el-form-item>
            <el-form-item class="basic-field full" label="流程分组" prop="groupId">
              <el-select v-model="definitionForm.groupId" filterable placeholder="请选择流程分组">
                <el-option v-for="item in groups" :key="item.id" :label="item.groupName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item class="basic-field full" prop="adminUsers">
              <template #label>
                <span class="field-label-with-help">
                  流程管理员
                  <el-tooltip content="用于流程维护；审批人为空且策略为转交管理员时，优先转交给这里配置的人员。" placement="top">
                    <el-icon><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <UserSelector v-model="definitionForm.adminUsers" multiple placeholder="请选择流程管理员" title="选择流程管理员" />
            </el-form-item>
            <el-form-item class="basic-field full" label="备注">
              <el-input v-model="definitionForm.remark" :rows="3" placeholder="说明流程适用场景、发起条件或维护边界" type="textarea" />
            </el-form-item>
          </el-form>
          <div class="pane-help">
            先选择分组再定义流程；发布、停用等状态由列表操作和发布动作维护。
          </div>
        </section>

        <section v-show="definitionStep === 1" class="builder-pane form-pane">
          <div class="form-design-shell">
            <div class="form-config-bar">
              <div class="form-config-main">
                <el-form class="form-code-form" label-position="top">
                  <el-form-item label="表单编码">
                    <el-input v-model="definitionForm.formCode" placeholder="如 guarantee_apply_form" />
                  </el-form-item>
                </el-form>
                <div class="form-type-row">
                  <div class="form-type-control">
                    <span>表单类型</span>
                    <el-radio-group v-model="workflowFormMode" @change="setWorkflowFormMode">
                      <el-radio-button label="DYNAMIC">动态表单</el-radio-button>
                      <el-radio-button label="CUSTOM">自定义表单</el-radio-button>
                    </el-radio-group>
                  </div>
                  <div class="form-designer-summary">
                    <strong>{{ workflowFormVariableOptions.length }}</strong>
                    <span>个字段变量</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="form-step-layout">
              <div class="form-designer-main">
                <template v-if="workflowFormMode === 'DYNAMIC'">
                  <div class="dynamic-designer-wrap" :class="{ 'hide-left': !formDesignerLeftVisible, 'hide-right': !formDesignerRightVisible }">
                    <div class="dynamic-designer-toolbar">
                      <div class="dynamic-designer-title">
                        <strong>动态表单设计器</strong>
                        <span>字段会转换为流程变量，可用于条件、权限和节点属性。</span>
                      </div>
                      <div class="dynamic-designer-actions">
                        <el-button :icon="formDesignerLeftVisible ? Fold : Expand" size="small" @click="formDesignerLeftVisible = !formDesignerLeftVisible">
                          {{ formDesignerLeftVisible ? '隐藏组件面板' : '显示组件面板' }}
                        </el-button>
                        <el-button :icon="formDesignerRightVisible ? Fold : Expand" size="small" @click="formDesignerRightVisible = !formDesignerRightVisible">
                          {{ formDesignerRightVisible ? '隐藏属性面板' : '显示属性面板' }}
                        </el-button>
                      </div>
                    </div>
                    <FcDesigner
                      ref="formDesignerRef"
                      :config="formDesignerConfig"
                      :menu="formDesignerMenu"
                      class="workflow-form-designer"
                      height="calc(100vh - 392px)"
                      @save="syncWorkflowFormFromDesigner"
                    />
                  </div>
                </template>
                <div v-else class="custom-form-builder">
                  <div class="custom-route-section">
                    <div class="section-title">自定义表单页面</div>
                    <el-form class="custom-route-form" label-position="top">
                      <el-form-item label="表单提交路径">
                        <el-input v-model="customFormConfig.submitPath" placeholder="例如：/flow/guarantee/create" @input="syncCustomWorkflowForm" />
                      </el-form-item>
                      <el-form-item label="表单查看路径">
                        <el-input v-model="customFormConfig.viewPath" placeholder="例如：/flow/guarantee/detail" @input="syncCustomWorkflowForm" />
                      </el-form-item>
                    </el-form>
                  </div>
                  <div class="custom-field-section">
                    <div class="custom-form-toolbar">
                      <div>
                        <div class="section-title">流程字段</div>
                        <span>字段会转换为流程变量，可在条件分支、表单权限、节点属性中选择。</span>
                      </div>
                      <el-button :icon="Plus" type="primary" @click="addCustomFormField">新增字段</el-button>
                    </div>
                    <div class="custom-field-list">
                      <div v-for="(field, index) in customFormFields" :key="field.key || index" class="custom-field-card">
                        <div class="custom-field-card-head">
                          <span>字段 {{ index + 1 }}</span>
                          <el-button link type="danger" @click="removeCustomFormField(index)">删除</el-button>
                        </div>
                        <div class="custom-field-main">
                          <el-form-item label="字段标识">
                            <el-input v-model="field.key" placeholder="如 amount" @blur="normalizeCustomFormFields" />
                          </el-form-item>
                          <el-form-item label="字段名称">
                            <el-input v-model="field.label" placeholder="如 流程金额" @input="syncCustomWorkflowForm" />
                          </el-form-item>
                          <el-form-item label="字段类型">
                            <el-select v-model="field.type" filterable @change="() => handleCustomFieldTypeChange(field)">
                              <el-option-group label="基础字段">
                                <el-option v-for="item in customFieldBaseTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                              </el-option-group>
                              <el-option-group label="系统数据">
                                <el-option v-for="item in customFieldSystemTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                              </el-option-group>
                            </el-select>
                          </el-form-item>
                          <el-form-item label="是否必填">
                            <el-switch v-model="field.required" active-text="必填" inactive-text="选填" @change="syncCustomWorkflowForm" />
                          </el-form-item>
                        </div>
                        <div class="custom-field-extra">
                          <template v-if="field.type === 'select'">
                            <label>选项配置</label>
                            <el-input
                              v-model="field.optionsText"
                              placeholder="标准流程=STANDARD,特殊流程=SPECIAL"
                              @input="syncCustomWorkflowForm"
                            />
                          </template>
                          <template v-else-if="isSystemCustomFieldType(field.type)">
                            <label>系统数据</label>
                            <el-select
                              :model-value="field.defaultValue"
                              clearable
                              filterable
                              :loading="customFieldSystemLoading(field.type)"
                              placeholder="可选默认值"
                              @focus="ensureCustomFieldSystemOptions(field.type)"
                              @visible-change="visible => visible && ensureCustomFieldSystemOptions(field.type)"
                              @change="value => updateCustomFieldDefault(field, value)"
                            >
                              <el-option v-for="item in customFieldSystemOptions(field.type)" :key="item.value" :label="item.label" :value="item.value" />
                            </el-select>
                          </template>
                          <template v-else>
                            <label>字段说明</label>
                            <el-input v-model="field.placeholder" placeholder="请输入字段提示文案" @input="syncCustomWorkflowForm" />
                          </template>
                        </div>
                      </div>
                      <el-empty v-if="!customFormFields.length" description="暂无流程字段" :image-size="72" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="pane-help">
            动态表单适合直接拖拽设计页面；自定义表单适合已有业务表单页面，只在这里维护流程变量字段。
          </div>
        </section>

        <section v-show="definitionStep === 2" class="builder-pane process-pane">
          <div class="designer-workbench">
            <div class="designer-body" :class="{ 'has-node-panel': nodePanelVisible && selectedNode }">
              <WorkflowDesignerCanvas
                :root="designerRoot"
                :catalog="nodeCatalog"
                :variable-groups="workflowVariableGroups"
                @select="selectNode"
                @changed="syncDesignerJson"
                @blank="clearSelectedNode"
              />

              <WorkflowNodePropertyPanel
                v-if="selectedNode"
                :visible="nodePanelVisible"
                :node="selectedNode"
                :title="nodeDrawerTitle"
                :type-label="workflowNodeTypeLabel(selectedNode)"
                :icon="nodeIcon(selectedNode)"
                @close="clearSelectedNode"
              >
                <el-tabs v-model="nodePropertyActiveTab" class="node-property-tabs">
                  <el-tab-pane label="基础信息" name="basic">
                    <el-form class="drawer-form compact-node-form" label-position="top">
                      <el-form-item label="节点名称">
                        <el-input v-model="selectedNode.nodeName" class="node-name-input" placeholder="请输入节点名称" @input="syncDesignerJson" />
                      </el-form-item>
                      <el-form-item label="节点类型">
                        <el-input :model-value="workflowNodeTypeLabel(selectedNode)" disabled />
                      </el-form-item>
                      <el-form-item label="节点说明">
                        <el-input v-model="selectedNode.description" :rows="3" placeholder="说明该节点的处理规则或业务含义" type="textarea" @input="syncDesignerJson" />
                      </el-form-item>
                      <div class="node-meta-strip">
                        <el-tag effect="plain" size="small">{{ workflowNodeTypeLabel(selectedNode) }}</el-tag>
                        <el-tag v-if="selectedNode.nodeDefinitionCode" effect="plain" size="small" type="info">{{ selectedNode.nodeDefinitionCode }}</el-tag>
                      </div>
                    </el-form>
                  </el-tab-pane>

                  <el-tab-pane label="表单权限" name="form">
                    <div v-if="workflowFormVariableOptions.length" class="form-permission-panel">
                      <div class="form-permission-head">
                        <span>表单字段</span>
                        <span>权限</span>
                      </div>
                      <div class="form-permission-list drawer-permission-list">
                        <div v-for="field in workflowFormVariableOptions" :key="field.value" class="form-permission-row">
                          <span class="form-permission-field">
                            <strong>{{ field.label }}</strong>
                            <small>{{ field.value }}</small>
                          </span>
                          <el-radio-group :model-value="nodeFieldPermission(selectedNode, field.value)" size="small" @change="value => updateNodeFieldPermission(selectedNode!, field.value, value as WorkflowFormPermission)">
                            <el-radio-button label="HIDDEN">隐藏</el-radio-button>
                            <el-radio-button label="READONLY">只读</el-radio-button>
                            <el-radio-button label="EDITABLE">编辑</el-radio-button>
                          </el-radio-group>
                        </div>
                      </div>
                    </div>
                    <el-empty v-else description="先在表单设计中添加字段" :image-size="72" />
                  </el-tab-pane>

                  <el-tab-pane label="节点属性" name="node">
                    <el-form class="drawer-form compact-node-form" label-position="top">
                      <template v-if="isRootNode(selectedNode)">
                        <WorkflowNodeRootConfig
                          :config="rootConfig(selectedNode)"
                          :user-options="approvalUserOptions"
                          :role-options="approvalRoleOptions"
                          :post-options="approvalPostOptions"
                          :org-tree-options="approvalOrgTreeOptions"
                          :target-loading="approvalTargetLoading"
                          @update="patch => updateRootConfig(selectedNode!, patch)"
                          @ensure-users="ensureApprovalUsersLoaded"
                          @ensure-roles="ensureApprovalRolesLoaded"
                          @ensure-posts="ensureApprovalPostsLoaded"
                          @ensure-orgs="ensureApprovalOrgsLoaded"
                        />
                      </template>
                      <template v-else-if="isUserTaskNode(selectedNode)">
                        <WorkflowNodeApprovalConfig
                          :config="approvalConfig(selectedNode)"
                          :assignee-type-options="approvalAssigneeTypeOptions"
                          :user-options="approvalUserOptions"
                          :role-options="approvalRoleOptions"
                          :post-options="approvalPostOptions"
                          :org-tree-options="approvalOrgTreeOptions"
                          :target-loading="approvalTargetLoading"
                          :form-variables="workflowFormVariableOptions"
                          :show-mode-config="showApprovalModeConfig(selectedNode)"
                          @update-assignee-type="value => updateApprovalAssigneeType(selectedNode!, value)"
                          @update-config="patch => updateApprovalConfig(selectedNode!, patch)"
                          @update-empty-strategy="value => updateEmptyAssigneeStrategy(selectedNode!, value)"
                          @update-list="(key, value) => updateApprovalListValue(selectedNode!, key, value)"
                          @ensure-users="ensureApprovalUsersLoaded"
                          @ensure-roles="ensureApprovalRolesLoaded"
                          @ensure-posts="ensureApprovalPostsLoaded"
                          @ensure-orgs="ensureApprovalOrgsLoaded"
                        />
                      </template>

                      <template v-else-if="selectedNode.nodeType === 'EXCLUSIVE_BRANCH'">
                          <WorkflowNodeConditionConfig
                            :node="selectedNode"
                            :groups="conditionGroups"
                            :variable-groups="workflowVariableGroups"
                            :mode="conditionEditMode(selectedNode)"
                            :user-options="approvalUserOptions"
                            :role-options="approvalRoleOptions"
                            :post-options="approvalPostOptions"
                            :org-tree-options="approvalOrgTreeOptions"
                            :target-loading="approvalTargetLoading"
                            @sync="syncDesignerJson"
                            @apply="applyConditionBuilder"
                            @update-mode="mode => updateConditionEditMode(selectedNode!, mode)"
                            @parse-expression="parseConditionToBuilder"
                            @add-group="addConditionGroup"
                            @add-row="addConditionRow"
                            @remove-group="removeConditionGroup"
                            @remove-row="removeConditionRow"
                            @ensure-users="ensureApprovalUsersLoaded"
                            @ensure-roles="ensureApprovalRolesLoaded"
                            @ensure-posts="ensureApprovalPostsLoaded"
                            @ensure-orgs="ensureApprovalOrgsLoaded"
                          />
                      </template>
                      <template v-else-if="isCcNode(selectedNode)">
                        <WorkflowNodeCcConfig
                          :config="ccConfig(selectedNode)"
                          :user-options="approvalUserOptions"
                          :role-options="approvalRoleOptions"
                          :post-options="approvalPostOptions"
                          :org-tree-options="approvalOrgTreeOptions"
                          :target-loading="approvalTargetLoading"
                          @update="patch => updateCcConfig(selectedNode!, patch)"
                          @ensure-users="ensureApprovalUsersLoaded"
                          @ensure-roles="ensureApprovalRolesLoaded"
                          @ensure-posts="ensureApprovalPostsLoaded"
                          @ensure-orgs="ensureApprovalOrgsLoaded"
                        />
                      </template>
                      <template v-else-if="isServiceTaskNode(selectedNode)">
                          <WorkflowNodeServiceConfig :node="selectedNode" @update="(key, value) => updateNodeProperty(selectedNode!, key, value)" />
                      </template>
                      <el-empty v-else description="当前节点暂无专属参数" :image-size="72" />
                    </el-form>
                  </el-tab-pane>

                  <el-tab-pane label="通知事件" name="notify">
                    <WorkflowNodeEventNotifyConfig
                      :config="nodeEventNotifyConfig(selectedNode)"
                      @update="patch => updateNodeEventNotifyConfig(selectedNode!, patch)"
                    />
                  </el-tab-pane>

                  <el-tab-pane label="扩展属性" name="advanced">
                    <WorkflowNodeAdvancedConfig
                      :model-value="advancedNodeProperties(selectedNode)"
                      :reserved-keys="reservedNodePropertyKeys(selectedNode)"
                      @update:model-value="value => updateAdvancedNodeProperties(selectedNode!, value)"
                    />
                  </el-tab-pane>

                  <el-tab-pane label="可用字段" name="variables">
                    <div v-if="nodePropertyActiveTab === 'variables'" class="variable-reference-panel">
                      <div v-for="group in workflowVariableGroups" :key="group.label" class="variable-reference-group">
                        <div class="variable-reference-source">{{ group.label }}</div>
                        <div class="variable-reference-list">
                          <div v-for="item in group.options" :key="item.value" class="variable-reference-item">
                            <strong>{{ item.value }}</strong>
                            <em>{{ item.label }}</em>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </WorkflowNodePropertyPanel>
            </div>
          </div>
        </section>
      </div>

      <div class="builder-footer">
        <el-button :disabled="definitionStep === 0" @click="definitionStep -= 1">上一步</el-button>
        <el-button v-if="definitionStep < 2" type="primary" @click="nextDefinitionStep">下一步</el-button>
        <el-button v-else :loading="publishing" type="primary" @click="publishDefinition">发布流程</el-button>
      </div>

      <el-dialog v-model="validateDialogShow" title="发布前检查" width="560px">
        <el-steps :active="validateFlowStep" finish-status="success" simple>
          <el-step title="基础信息" />
          <el-step title="表单信息" />
          <el-step title="流程设计" />
        </el-steps>

        <div class="validate-result">
          <el-result v-if="validateErrMsg.length" icon="error" title="检查失败">
            <template #sub-title>
              <div v-for="item in validateErrMsg" :key="item" class="validate-error">{{ item }}</div>
            </template>
            <template #extra>
              <el-button type="primary" @click="gotoValidateError">返回修改</el-button>
            </template>
          </el-result>
          <el-result v-else-if="validateFlowStep >= 3" icon="success" title="检查通过" sub-title="基础信息、表单信息和流程设计均已通过检查。">
            <template #extra>
              <el-button :loading="publishing" type="primary" @click="submitPublishDefinition">确认发布</el-button>
            </template>
          </el-result>
          <el-result v-else title="正在检查" sub-title="系统正在按步骤校验流程配置。">
            <template #icon>
              <span class="validate-loading" v-loading="true" />
            </template>
          </el-result>
        </div>
      </el-dialog>
    </div>

    <el-tabs v-else v-model="activeTab" class="workflow-tabs">
      <el-tab-pane label="流程定义" name="definitions">
        <div class="page-toolbar">
          <el-form :inline="true" :model="definitionQuery" class="query-form">
            <el-form-item label="关键字">
              <el-input v-model="definitionQuery.keyword" clearable placeholder="流程名称/编码" />
            </el-form-item>
            <el-form-item label="流程分组">
              <el-select v-model="definitionQuery.groupId" clearable filterable placeholder="全部分组">
                <el-option v-for="item in groups" :key="item.id" :label="item.groupName" :value="item.id!" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="definitionQuery.status" clearable placeholder="全部状态">
                <el-option v-for="item in workflowStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button :icon="Search" type="primary" @click="loadDefinitions">查询</el-button>
              <el-button :icon="Refresh" @click="resetDefinitionQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="workflow-definition-actions action-toolbar">
          <div class="toolbar-left">
            <el-button :icon="Plus" type="primary" @click="openDefinitionForm()">创建流程</el-button>
            <el-button :icon="Plus" @click="openGroupForm()">创建分组</el-button>
          </div>
        </div>

        <el-table v-loading="definitionLoading" :data="definitions" border>
          <el-table-column label="流程名称" min-width="180" prop="definitionName" />
          <el-table-column label="流程编码" min-width="180" prop="definitionKey" />
          <el-table-column label="分组" min-width="120" prop="groupName" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="workflowStatusType(row.status)">{{ workflowStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布版本" width="100" prop="publishedVersionNo" />
          <el-table-column label="引擎版本" width="100" prop="processDefinitionVersion" />
          <el-table-column label="最后发布" width="170" prop="lastDeployTime" />
          <el-table-column label="更新时间" width="170" prop="updatedTime" />
          <el-table-column fixed="right" label="操作" width="330">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDefinitionForm(row)">设计</el-button>
              <el-button link type="success" @click="deployDefinition(row)">发布</el-button>
              <el-button link type="primary" @click="openVersionDrawer(row)">版本</el-button>
              <el-button link type="warning" @click="toggleDefinitionStatus(row)">
                {{ row.status === 'DISABLED' ? '启用' : '停用' }}
              </el-button>
              <el-button link type="danger" @click="deleteDefinition(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="definitionQuery.pageNum"
            v-model:page-size="definitionQuery.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="definitionTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadDefinitions"
            @size-change="loadDefinitions"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="流程分组" name="groups">
        <div class="page-toolbar">
          <el-form :inline="true" :model="groupQuery" class="query-form">
            <el-form-item label="关键字">
              <el-input v-model="groupQuery.keyword" clearable placeholder="分组名称/编码" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="groupQuery.status" clearable placeholder="全部状态">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button :icon="Search" type="primary" @click="loadGroupsPage">查询</el-button>
              <el-button :icon="Refresh" @click="resetGroupQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-button :icon="Plus" type="primary" @click="openGroupForm()">新增分组</el-button>
        </div>

        <el-table v-loading="groupLoading" :data="groupRows" border>
          <el-table-column label="分组名称" min-width="160" prop="groupName" />
          <el-table-column label="分组编码" min-width="160" prop="groupCode" />
          <el-table-column label="排序" width="90" prop="sort" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" prop="remark" show-overflow-tooltip />
          <el-table-column fixed="right" label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openGroupForm(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteGroup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="groupQuery.pageNum"
            v-model:page-size="groupQuery.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="groupTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadGroupsPage"
            @size-change="loadGroupsPage"
          />
        </div>
      </el-tab-pane>

    </el-tabs>

    <el-drawer v-model="versionDrawer" title="发布版本" size="720px">
      <el-table v-loading="versionLoading" :data="versions" border>
        <el-table-column label="版本号" width="90" prop="versionNo" />
        <el-table-column label="发布状态" width="110" prop="publishStatus" />
        <el-table-column label="引擎版本" width="100" prop="processDefinitionVersion" />
        <el-table-column label="Deployment ID" min-width="170" prop="deploymentId" show-overflow-tooltip />
        <el-table-column label="发布时间" width="170" prop="publishTime" />
      </el-table>
      <el-collapse class="xml-collapse">
        <el-collapse-item title="最近一次发布 BPMN XML" name="xml">
          <el-input :model-value="currentVersionXml" :rows="16" readonly type="textarea" />
        </el-collapse-item>
      </el-collapse>
    </el-drawer>

    <el-dialog v-model="groupDialog" :title="groupForm.id ? '编辑流程分组' : '新增流程分组'" width="520px">
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="100px">
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="groupForm.groupName" />
        </el-form-item>
        <el-form-item label="分组编码" prop="groupCode">
          <el-input v-model="groupForm.groupCode" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="groupForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="groupEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="groupForm.remark" :rows="3" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Bell, Box, Cloudy, Connection, Expand, Fold, ForkSpoon, PictureFilled, Plus, QuestionFilled, Refresh, Search, Setting, Share, User } from '@element-plus/icons-vue';
import FcDesigner, { type Config as FcDesignerConfig } from 'form-create-designer';
import type { Rule as FcRule } from '@form-create/element-ui';
import 'form-create-designer/src/style/index.css';
import 'form-create-designer/src/style/icon.css';
import { ImageUpload, UserSelector, get } from '@mango/common';
import WorkflowDesignerCanvas from './components/workflow-designer/WorkflowDesignerCanvas.vue';
import WorkflowNodeAdvancedConfig from './components/workflow-designer/WorkflowNodeAdvancedConfig.vue';
import WorkflowNodeApprovalConfig from './components/workflow-designer/WorkflowNodeApprovalConfig.vue';
import WorkflowNodeCcConfig from './components/workflow-designer/WorkflowNodeCcConfig.vue';
import WorkflowNodeConditionConfig from './components/workflow-designer/WorkflowNodeConditionConfig.vue';
import WorkflowNodeEventNotifyConfig from './components/workflow-designer/WorkflowNodeEventNotifyConfig.vue';
import WorkflowNodePropertyPanel from './components/workflow-designer/WorkflowNodePropertyPanel.vue';
import WorkflowNodeRootConfig from './components/workflow-designer/WorkflowNodeRootConfig.vue';
import WorkflowNodeServiceConfig from './components/workflow-designer/WorkflowNodeServiceConfig.vue';
import type {
  ApprovalOrgTreeOption,
  ApprovalTargetOption,
  ConditionEditMode,
  ConditionGroup,
  ConditionRow,
  WorkflowVariableDataType,
  WorkflowVariableGroup,
  WorkflowVariableOption,
} from './components/workflow-designer/types';
import {
  createNodeId,
  defaultApprovalConfig,
  defaultDesignerJson,
  parseDesignerJson,
  stringifyDesignerJson,
  workflowApi,
  workflowStatusLabel,
  workflowStatusOptions,
  workflowStatusType,
  type WorkflowDefinition,
  type WorkflowDefinitionVersion,
  type WorkflowApprovalNodeConfig,
  type WorkflowDesignerNode,
  type WorkflowEventNotifyConfig,
  type WorkflowFormPermission,
  type WorkflowGroup,
  type WorkflowId,
  type WorkflowNodeCatalog,
  type WorkflowStatus,
} from '../../api/workflow';

type WorkflowFormMode = 'DYNAMIC' | 'CUSTOM';

interface CustomFormField {
  key: string;
  label: string;
  type: string;
  required: boolean;
  optionsText?: string;
  placeholder?: string;
  defaultValue?: string;
}

interface CustomFormConfig {
  submitPath: string;
  viewPath: string;
}

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
}

const NODE_ICON_MAP: Record<string, any> = {
  ROOT: User,
  APPROVAL: User,
  CC: Bell,
  EXCLUSIVE_GATEWAY: ForkSpoon,
  PARALLEL_GATEWAY: Share,
  SERVICE: Setting,
  SERVICE_BEAN: Box,
  SERVICE_HTTP: Cloudy,
  SERVICE_REMOTE: Connection,
  EVENT_PUBLISH: Bell,
  EXCLUSIVE_BRANCH: Share,
};

function workflowNodeTypeLabel(node: Partial<WorkflowDesignerNode>) {
  if (node.nodeType === 'ROOT') return '发起节点';
  if (node.nodeType === 'APPROVAL' || node.executionType === 'USER_TASK' || node.bpmnType === 'userTask') return '审批节点';
  if (node.nodeType === 'CC') return '抄送节点';
  if (node.nodeType === 'EXCLUSIVE_GATEWAY') return '条件分支';
  if (node.nodeType === 'PARALLEL_GATEWAY') return '条件分支';
  if (node.nodeType === 'EXCLUSIVE_BRANCH') return '分支条件';
  if (node.nodeType === 'SERVICE' || node.bpmnType === 'serviceTask' || node.executionType) return '服务节点';
  return '流程节点';
}

const workflowIconOptions = [
  { value: 'User', label: '人员流程', component: User },
  { value: 'Bell', label: '通知提醒', component: Bell },
  { value: 'ForkSpoon', label: '条件审批', component: ForkSpoon },
  { value: 'Share', label: '并行协作', component: Share },
  { value: 'Box', label: '服务处理', component: Box },
  { value: 'Cloudy', label: '接口流程', component: Cloudy },
  { value: 'Connection', label: '外部连接', component: Connection },
];

const formDesignerConfig: FcDesignerConfig = {
  fieldReadonly: false,
  showSaveBtn: true,
  showDevice: false,
  showLanguage: false,
  showInputData: false,
  hiddenMenu: ['layout'],
};

type FormDesignerMenuItem = {
  name: string;
  label?: string;
  icon?: string;
};

type FormDesignerMenu = {
  name: string;
  title: string;
  hidden?: boolean;
  list: FormDesignerMenuItem[];
};

type WorkflowBusinessComponent = FormDesignerMenuItem & {
  menu: 'business';
  input: boolean;
  event?: string[];
  validate?: string[];
  languageKey: string[];
  rule: () => FcRule;
  props: () => any[];
};

const formDesignerMenu: FormDesignerMenu[] = [
  {
    name: 'business',
    title: '业务组件',
    list: [],
  },
  {
    name: 'aide',
    title: '辅助组件',
    list: [
      { name: 'elAlert', label: '提示', icon: 'icon-alert' },
      { name: 'text', label: '文字', icon: 'icon-text' },
      { name: 'html', label: 'HTML', icon: 'icon-html' },
      { name: 'elDivider', label: '分割线', icon: 'icon-divider' },
      { name: 'elButton', label: '按钮', icon: 'icon-button' },
      { name: 'elTag', label: '标签', icon: 'icon-tag' },
      { name: 'elImage', label: '图片展示', icon: 'icon-image' },
    ],
  },
  {
    name: 'subform',
    title: '子表单组件',
    list: [
      { name: 'group', label: '对象容器', icon: 'icon-group' },
      { name: 'subForm', label: '子表单', icon: 'icon-subform' },
      { name: 'tableForm', label: '表格子表单', icon: 'icon-table' },
    ],
  },
  {
    name: 'main',
    title: '基础组件',
    list: [
      { name: 'input', label: '输入框', icon: 'icon-input' },
      { name: 'textarea', label: '多行输入框', icon: 'icon-textarea' },
      { name: 'password', label: '密码输入框', icon: 'icon-password' },
      { name: 'inputNumber', label: '计数器', icon: 'icon-number' },
      { name: 'radio', label: '单选框', icon: 'icon-radio' },
      { name: 'checkbox', label: '多选框', icon: 'icon-checkbox' },
      { name: 'select', label: '选择器', icon: 'icon-select' },
      { name: 'switch', label: '开关', icon: 'icon-switch' },
      { name: 'rate', label: '评分', icon: 'icon-rate' },
      { name: 'timePicker', label: '时间', icon: 'icon-time' },
      { name: 'timeRange', label: '时间区间', icon: 'icon-time-range' },
      { name: 'slider', label: '滑块', icon: 'icon-slider' },
      { name: 'datePicker', label: '日期', icon: 'icon-date' },
      { name: 'dateRange', label: '日期区间', icon: 'icon-date-range' },
      { name: 'colorPicker', label: '颜色选择器', icon: 'icon-color' },
      { name: 'cascader', label: '级联选择器', icon: 'icon-cascader' },
      { name: 'upload', label: '上传', icon: 'icon-upload' },
      { name: 'elTransfer', label: '穿梭框', icon: 'icon-transfer' },
      { name: 'tree', label: '树形控件', icon: 'icon-tree' },
      { name: 'elTreeSelect', label: '树形选择', icon: 'icon-tree-select' },
      { name: 'fcEditor', label: '富文本', icon: 'icon-editor' },
    ],
  },
];

function createWorkflowBusinessField(prefix: string) {
  return `${prefix}_${Math.random().toString(36).slice(2, 8)}`;
}

function createWorkflowBusinessComponent(
  name: string,
  label: string,
  icon: string,
  ruleFactory: () => FcRule,
  validate: string[] = ['string'],
): WorkflowBusinessComponent {
  return {
    name,
    label,
    icon,
    menu: 'business',
    input: true,
    event: ['change', 'blur', 'focus', 'clear'],
    validate,
    languageKey: [],
    rule: ruleFactory,
    props: () => [],
  };
}

function toFormCreateOption(item: ApprovalTargetOption) {
  return { label: item.label, value: item.value };
}

function toWorkflowBusinessMenuItems(): FormDesignerMenuItem[] {
  return workflowBusinessFormComponents.map(({ name, label, icon }) => ({ name, label, icon }));
}

const workflowBusinessFormComponents: WorkflowBusinessComponent[] = [
  createWorkflowBusinessComponent('workflowUser', '人员', 'icon-user', () => ({
    type: 'select',
    field: createWorkflowBusinessField('userId'),
    title: '人员',
    props: {
      placeholder: '请选择人员',
      clearable: true,
      filterable: true,
      workflowDataType: 'systemUser',
    },
    options: approvalUserOptions.value.map(toFormCreateOption),
  })),
  createWorkflowBusinessComponent('workflowOrg', '部门', 'icon-tree', () => ({
    type: 'elTreeSelect',
    field: createWorkflowBusinessField('orgId'),
    title: '部门',
    props: {
      placeholder: '请选择部门',
      clearable: true,
      filterable: true,
      nodeKey: 'value',
      checkStrictly: true,
      workflowDataType: 'systemOrg',
      data: approvalOrgTreeOptions.value,
    },
  }), ['string', 'number', 'array']),
  createWorkflowBusinessComponent('workflowPost', '岗位', 'icon-tag', () => ({
    type: 'select',
    field: createWorkflowBusinessField('postId'),
    title: '岗位',
    props: {
      placeholder: '请选择岗位',
      clearable: true,
      filterable: true,
      workflowDataType: 'systemPost',
    },
    options: approvalPostOptions.value.map(toFormCreateOption),
  })),
  createWorkflowBusinessComponent('workflowRole', '角色', 'icon-group', () => ({
    type: 'select',
    field: createWorkflowBusinessField('roleId'),
    title: '角色',
    props: {
      placeholder: '请选择角色',
      clearable: true,
      filterable: true,
      workflowDataType: 'systemRole',
    },
    options: approvalRoleOptions.value.map(toFormCreateOption),
  })),
  createWorkflowBusinessComponent('workflowUpload', '上传', 'icon-upload', () => ({
    type: 'upload',
    field: createWorkflowBusinessField('attachment'),
    title: '上传',
    props: {
      action: '/api/file/files',
      limit: 5,
      multiple: true,
    },
  }), ['array']),
  createWorkflowBusinessComponent('workflowImage', '图片', 'icon-image', () => ({
    type: 'upload',
    field: createWorkflowBusinessField('image'),
    title: '图片',
    props: {
      action: '/api/file/files',
      listType: 'picture-card',
      accept: 'image/*',
      limit: 6,
    },
  }), ['array']),
  createWorkflowBusinessComponent('workflowArea', '地区', 'icon-cascader', () => ({
    type: 'cascader',
    field: createWorkflowBusinessField('areaCode'),
    title: '地区',
    props: {
      placeholder: '请选择地区',
      clearable: true,
      filterable: true,
      options: [],
    },
  }), ['string', 'number', 'array']),
  createWorkflowBusinessComponent('workflowSignature', '签名', 'icon-edit', () => ({
    type: 'input',
    field: createWorkflowBusinessField('signature'),
    title: '签名',
    props: {
      placeholder: '请完成签名',
      readonly: true,
    },
  })),
  createWorkflowBusinessComponent('workflowDict', '字典', 'icon-select', () => ({
    type: 'select',
    field: createWorkflowBusinessField('dictValue'),
    title: '字典',
    props: {
      placeholder: '请选择字典值',
      clearable: true,
      filterable: true,
    },
    options: [],
  })),
  createWorkflowBusinessComponent('workflowSerialNo', '流水号', 'icon-number', () => ({
    type: 'input',
    field: createWorkflowBusinessField('serialNo'),
    title: '流水号',
    props: {
      placeholder: '系统自动生成',
      readonly: true,
    },
  })),
];

const defaultWorkflowFormRules = (): FcRule[] => [
  {
    type: 'inputNumber',
    field: 'amount',
    title: '流程金额',
    props: {
      placeholder: '请输入流程金额',
      controlsPosition: 'right',
    },
    validate: [
      { required: true, message: '流程金额不能为空', trigger: 'change' },
    ],
  },
  {
    type: 'select',
    field: 'applyType',
    title: '流程类型',
    props: {
      placeholder: '请选择流程类型',
      clearable: true,
    },
    options: [
      { label: '标准流程', value: 'STANDARD' },
      { label: '特殊流程', value: 'SPECIAL' },
    ],
  },
];

const activeTab = ref('definitions');
const definitionLoading = ref(false);
const groupLoading = ref(false);
const saving = ref(false);
const versionLoading = ref(false);

const groups = ref<WorkflowGroup[]>([]);
const groupRows = ref<WorkflowGroup[]>([]);
const definitions = ref<WorkflowDefinition[]>([]);
const nodeCatalog = ref<WorkflowNodeCatalog[]>([]);
const versions = ref<WorkflowDefinitionVersion[]>([]);
const currentVersionXml = ref('');
const definitionTotal = ref(0);
const groupTotal = ref(0);

const definitionQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', groupId: '' as WorkflowId | '', status: '' });
const groupQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' as number | '' });

const designerMode = ref(false);
const publishing = ref(false);
const validateDialogShow = ref(false);
const validateFlowStep = ref(0);
const validateErrMsg = ref<string[]>([]);
const groupDialog = ref(false);
const versionDrawer = ref(false);
const definitionFormRef = ref<FormInstance>();
const groupFormRef = ref<FormInstance>();
const formDesignerRef = ref<InstanceType<typeof FcDesigner>>();
const designerRoot = ref<WorkflowDesignerNode>(parseDesignerJson(defaultDesignerJson()));
const selectedNode = ref<WorkflowDesignerNode>();
const nodePanelVisible = ref(false);
const nodePropertyActiveTab = ref('node');
const definitionStep = ref(0);
const conditionGroups = ref<ConditionGroup[]>([createConditionGroup()]);
const workflowFormMode = ref<WorkflowFormMode>('DYNAMIC');
const workflowFormRules = ref<FcRule[]>(defaultWorkflowFormRules());
const customFormFields = ref<CustomFormField[]>([]);
const customFormConfig = reactive<CustomFormConfig>({ submitPath: '', viewPath: '' });
const formDesignerLeftVisible = ref(true);
const formDesignerRightVisible = ref(true);
const designerSteps = [
  { key: 'basic', title: '基础信息', description: '名称、编码、分组' },
  { key: 'form', title: '表单信息', description: '表单模式与变量' },
  { key: 'process', title: '流程设计', description: '节点、分支、执行动作' },
];

const definitionForm = reactive<WorkflowDefinition>({
  groupId: '',
  adminUsers: [],
  icon: '',
  definitionName: '',
  definitionKey: '',
  designerJson: defaultDesignerJson(),
  status: 'DRAFT',
});

const groupForm = reactive<WorkflowGroup>({
  groupName: '',
  groupCode: '',
  sort: 0,
  status: 1,
});

const groupEnabled = computed({
  get: () => groupForm.status === 1,
  set: value => { groupForm.status = value ? 1 : 0; },
});

const workflowApplicantVariableOptions: WorkflowVariableOption[] = [
  { label: '申请人ID', value: 'applicant.id', source: '申请人', dataType: 'USER' },
  { label: '申请人姓名', value: 'applicant.name', source: '申请人', dataType: 'TEXT' },
  { label: '申请人账号', value: 'applicant.username', source: '申请人', dataType: 'TEXT' },
  { label: '申请人手机号', value: 'applicant.mobile', source: '申请人', dataType: 'TEXT' },
  { label: '申请人部门ID', value: 'applicant.orgId', source: '申请人', dataType: 'ORG' },
  { label: '申请人部门名称', value: 'applicant.orgName', source: '申请人', dataType: 'TEXT' },
  { label: '申请人岗位ID', value: 'applicant.postId', source: '申请人', dataType: 'POST' },
  { label: '申请人岗位名称', value: 'applicant.postName', source: '申请人', dataType: 'TEXT' },
  { label: '申请人角色ID列表', value: 'applicant.roleIds', source: '申请人', dataType: 'ROLE' },
  { label: '申请人角色编码列表', value: 'applicant.roleCodes', source: '申请人' },
];

const workflowSystemVariableOptions: WorkflowVariableOption[] = [
  { label: '当前机构ID', value: 'tenantId', source: '系统内置', dataType: 'TEXT' },
  { label: '当前机构编码', value: 'tenantCode', source: '系统内置', dataType: 'TEXT' },
  { label: '流程定义编码', value: 'definitionKey', source: '系统内置', dataType: 'TEXT' },
  { label: '流程发起人', value: 'initiator', source: '系统内置', dataType: 'USER' },
  { label: '流程启动时间', value: 'startTime', source: '系统内置', dataType: 'DATE' },
  { label: '业务主键', value: 'businessKey', source: '系统内置', dataType: 'TEXT' },
];

const workflowFormVariableOptions = computed(() => collectWorkflowFormVariables(workflowFormRules.value));

const workflowVariableGroups = computed<WorkflowVariableGroup[]>(() => [
  { label: '表单字段', options: workflowFormVariableOptions.value },
  { label: '申请人基础信息', options: workflowApplicantVariableOptions },
  { label: '系统内置参数', options: workflowSystemVariableOptions },
].filter(group => group.options.length > 0));

const approvalAssigneeTypeOptions = [
  { label: '指定成员', value: 'SPECIFIED_USER' },
  { label: '部门主管', value: 'ORG_LEADER' },
  { label: '角色', value: 'SPECIFIED_ROLE' },
  { label: '指定岗位', value: 'SPECIFIED_POST' },
  { label: '指定组织', value: 'SPECIFIED_ORG' },
  { label: '发起人自选', value: 'INITIATOR_SELECT' },
  { label: '发起人自己', value: 'INITIATOR' },
  { label: '表单人员', value: 'FORM_USER' },
  { label: '流程表达式', value: 'EXPRESSION' },
];

const customFieldBaseTypeOptions = [
  { label: '单行文本', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字', value: 'inputNumber' },
  { label: '下拉选项', value: 'select' },
  { label: '日期', value: 'datePicker' },
];

const customFieldSystemTypeOptions = [
  { label: '人员', value: 'systemUser' },
  { label: '部门', value: 'systemOrg' },
  { label: '岗位', value: 'systemPost' },
  { label: '角色', value: 'systemRole' },
];

const nodeDrawerTitle = computed(() => {
  if (!selectedNode.value) {
    return '节点属性';
  }
  if (isUserTaskNode(selectedNode.value)) {
    return '节点配置';
  }
  if (selectedNode.value.nodeType === 'EXCLUSIVE_BRANCH') {
    return '条件设置';
  }
  return '节点属性';
});

function clearSelectedNode() {
  selectedNode.value = undefined;
  nodePanelVisible.value = false;
}

const approvalUserOptions = ref<ApprovalTargetOption[]>([]);
const approvalRoleOptions = ref<ApprovalTargetOption[]>([]);
const approvalPostOptions = ref<ApprovalTargetOption[]>([]);
const approvalOrgTreeOptions = ref<ApprovalOrgTreeOption[]>([]);
const approvalTargetLoaded = reactive({
  users: false,
  roles: false,
  posts: false,
  orgs: false,
});
const approvalTargetLoading = reactive({
  users: false,
  roles: false,
  posts: false,
  orgs: false,
});

function validateDefinitionGroup(_rule: unknown, value: unknown, callback: (error?: Error) => void) {
  const groupId = String(value || '').trim();
  if (!groupId) {
    callback(new Error('请选择流程分组'));
    return;
  }
  if (!groups.value.some(item => String(item.id) === groupId)) {
    callback(new Error('流程分组不存在或已停用，请重新选择'));
    return;
  }
  callback();
}

const definitionRules: FormRules = {
  groupId: [{ validator: validateDefinitionGroup, trigger: 'change' }],
  definitionName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  definitionKey: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  designerJson: [{ required: true, message: '请设计流程节点', trigger: 'blur' }],
};

const groupRules: FormRules = {
  groupName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
  groupCode: [{ required: true, message: '请输入分组编码', trigger: 'blur' }],
};

onMounted(async () => {
  await Promise.all([loadGroupOptions(), loadNodeCatalog()]);
  await Promise.all([loadDefinitions(), loadGroupsPage()]);
});

async function registerWorkflowBusinessFormComponents() {
  await nextTick();
  const designer = formDesignerRef.value as any;
  if (!designer) {
    return;
  }
  await ensureWorkflowBusinessFormDataLoaded();
  if (designer.setupState?.dragRuleList) {
    workflowBusinessFormComponents.forEach(component => {
      designer.setupState.dragRuleList[component.name] = component;
    });
  } else {
    designer.addComponent?.(workflowBusinessFormComponents.map(component => ({ ...component, menu: undefined })));
  }
  designer.setMenuItem?.('business', toWorkflowBusinessMenuItems());
}

async function ensureWorkflowBusinessFormDataLoaded() {
  await Promise.all([
    ensureApprovalUsersLoaded(),
    ensureApprovalRolesLoaded(),
    ensureApprovalPostsLoaded(),
    ensureApprovalOrgsLoaded(),
  ]);
}

async function loadNodeCatalog() {
  nodeCatalog.value = await workflowApi.nodeCatalog();
}

async function loadGroupOptions() {
  groups.value = await workflowApi.groupsList(1);
}

async function loadDefinitions() {
  definitionLoading.value = true;
  try {
    const page = await workflowApi.definitionsPage(definitionQuery);
    definitions.value = page.list;
    definitionTotal.value = page.total;
  } finally {
    definitionLoading.value = false;
  }
}

async function loadGroupsPage() {
  groupLoading.value = true;
  try {
    const page = await workflowApi.groupsPage(groupQuery as any);
    groupRows.value = page.list;
    groupTotal.value = page.total;
  } finally {
    groupLoading.value = false;
  }
}

function resetDefinitionQuery() {
  Object.assign(definitionQuery, { pageNum: 1, pageSize: 10, keyword: '', groupId: '', status: '' });
  loadDefinitions();
}

function resetGroupQuery() {
  Object.assign(groupQuery, { pageNum: 1, pageSize: 10, keyword: '', status: '' });
  loadGroupsPage();
}

async function openDefinitionForm(row?: WorkflowDefinition) {
  await Promise.all([loadGroupOptions(), loadNodeCatalog()]);
  Object.assign(definitionForm, row || {
    id: undefined,
    groupId: groups.value[0]?.id || '',
    adminUsers: [],
    icon: '',
    definitionName: '',
    definitionKey: '',
    formCode: '',
    formJson: JSON.stringify(defaultWorkflowFormRules(), null, 2),
    designerJson: defaultDesignerJson(),
    bpmnXml: '',
    status: 'DRAFT' as WorkflowStatus,
    remark: '',
  });
  loadWorkflowFormConfig(definitionForm.formJson);
  designerRoot.value = parseDesignerJson(definitionForm.designerJson);
  selectedNode.value = undefined;
  nodePanelVisible.value = false;
  definitionStep.value = 0;
  syncDesignerJson();
  definitionForm.formJson = stringifyWorkflowFormConfig();
  designerMode.value = true;
  await nextTick();
  await registerWorkflowBusinessFormComponents();
  applyFormRulesToDesigner();
}

async function closeDesigner() {
  designerMode.value = false;
  validateDialogShow.value = false;
  await loadDefinitions();
}

function selectNode(node: WorkflowDesignerNode) {
  selectedNode.value = node;
  nodePropertyActiveTab.value = hasNodeSpecificConfig(node) ? 'node' : 'basic';
  nodePanelVisible.value = true;
  if (node.nodeType === 'EXCLUSIVE_BRANCH') {
    parseConditionToBuilder(node.conditionExpression || '');
  }
}

function syncDesignerJson() {
  definitionForm.designerJson = stringifyDesignerJson(designerRoot.value);
}

async function persistDefinition() {
  syncDesignerJson();
  syncCurrentWorkflowForm();
  definitionForm.formJson = stringifyWorkflowFormConfig();
  await definitionFormRef.value?.validate();
  saving.value = true;
  try {
    if (definitionForm.id) {
      await workflowApi.updateDefinition(definitionForm);
    } else {
      definitionForm.id = await workflowApi.createDefinition(definitionForm);
    }
    await Promise.all([loadDefinitions(), loadGroupOptions()]);
    return definitionForm.id;
  } finally {
    saving.value = false;
  }
}

async function saveDefinitionDraft() {
  await persistDefinition();
  ElMessage.success('草稿已保存');
}

async function nextDefinitionStep() {
  if (!(await validateStep(definitionStep.value, false))) {
    return;
  }
  if (definitionStep.value === 1) {
    syncCurrentWorkflowForm();
    definitionForm.formJson = stringifyWorkflowFormConfig();
    await nextTick();
  }
  definitionStep.value = Math.min(definitionStep.value + 1, 2);
}

async function goDefinitionStep(targetStep: number) {
  if (targetStep <= definitionStep.value) {
    definitionStep.value = targetStep;
    return;
  }
  for (let step = 0; step < targetStep; step += 1) {
    if (!(await validateStep(step, true))) {
      definitionStep.value = step;
      return;
    }
  }
  if (targetStep >= 2) {
    syncCurrentWorkflowForm();
    definitionForm.formJson = stringifyWorkflowFormConfig();
    await nextTick();
  }
  definitionStep.value = targetStep;
}

async function validateStep(step: number, silent: boolean) {
  if (step === 0) {
    try {
      await definitionFormRef.value?.validate();
      return true;
    } catch {
      if (!silent) {
        ElMessage.warning('请先完善基础信息');
      }
      return false;
    }
  }
  if (step === 1) {
    return validateWorkflowForm(silent);
  }
  const errors = validateDesignerTree();
  if (errors.length > 0) {
    if (!silent) {
      ElMessage.warning(errors[0]);
    }
    return false;
  }
  return true;
}

async function deployDefinition(row: WorkflowDefinition) {
  await ElMessageBox.confirm(`确认发布流程「${row.definitionName}」？`, '发布流程', { type: 'warning' });
  await workflowApi.deployDefinition(row.id!);
  ElMessage.success('发布成功');
  await loadDefinitions();
}

async function publishDefinition() {
  validateErrMsg.value = [];
  validateFlowStep.value = 0;
  validateDialogShow.value = true;

  if (!(await validateStep(0, true))) {
    validateErrMsg.value = ['请完善基础信息'];
    validateFlowStep.value = 0;
    return;
  }
  validateFlowStep.value = 1;

  if (!(await validateStep(1, true))) {
    validateErrMsg.value = ['请完善表单信息'];
    validateFlowStep.value = 1;
    return;
  }
  validateFlowStep.value = 2;

  const nodeErrors = validateDesignerTree();
  if (nodeErrors.length > 0) {
    validateErrMsg.value = nodeErrors;
    validateFlowStep.value = 2;
    return;
  }
  validateFlowStep.value = 3;
}

async function submitPublishDefinition() {
  publishing.value = true;
  try {
    const id = await persistDefinition();
    await workflowApi.deployDefinition(id!);
    ElMessage.success('发布成功');
    validateDialogShow.value = false;
    designerMode.value = false;
    await loadDefinitions();
  } finally {
    publishing.value = false;
  }
}

function gotoValidateError() {
  definitionStep.value = Math.min(validateFlowStep.value, 2);
  validateDialogShow.value = false;
}

async function openVersionDrawer(row: WorkflowDefinition) {
  versionDrawer.value = true;
  versionLoading.value = true;
  currentVersionXml.value = '';
  try {
    versions.value = await workflowApi.definitionVersions(row.id!);
    currentVersionXml.value = versions.value[0]?.bpmnXml || row.bpmnXml || '';
  } finally {
    versionLoading.value = false;
  }
}

async function toggleDefinitionStatus(row: WorkflowDefinition) {
  const status: WorkflowStatus = row.status === 'DISABLED' ? 'DRAFT' : 'DISABLED';
  await workflowApi.updateDefinitionStatus(row.id!, status);
  ElMessage.success('状态已更新');
  await loadDefinitions();
}

async function deleteDefinition(row: WorkflowDefinition) {
  await ElMessageBox.confirm(`确认删除流程「${row.definitionName}」？`, '删除流程', { type: 'warning' });
  await workflowApi.deleteDefinition(row.id!);
  ElMessage.success('删除成功');
  await loadDefinitions();
}

function openGroupForm(row?: WorkflowGroup) {
  Object.assign(groupForm, row || { id: undefined, groupName: '', groupCode: '', sort: 0, status: 1, remark: '' });
  groupDialog.value = true;
}

async function saveGroup() {
  await groupFormRef.value?.validate();
  saving.value = true;
  try {
    if (groupForm.id) {
      await workflowApi.updateGroup(groupForm);
    } else {
      await workflowApi.createGroup(groupForm);
    }
    ElMessage.success('保存成功');
    groupDialog.value = false;
    await Promise.all([loadGroupsPage(), loadGroupOptions()]);
  } finally {
    saving.value = false;
  }
}

async function deleteGroup(row: WorkflowGroup) {
  await ElMessageBox.confirm(`确认删除分组「${row.groupName}」？`, '删除分组', { type: 'warning' });
  await workflowApi.deleteGroup(row.id!);
  ElMessage.success('删除成功');
  await Promise.all([loadGroupsPage(), loadGroupOptions()]);
}

function nodeIcon(item: Partial<WorkflowNodeCatalog | WorkflowDesignerNode>) {
  const iconName = 'icon' in item ? item.icon : undefined;
  if (iconName && iconName in NODE_ICON_MAP) {
    return NODE_ICON_MAP[iconName];
  }
  return NODE_ICON_MAP[item.nodeType || ''] || NODE_ICON_MAP[item.executionType || ''] || Setting;
}

function workflowIconComponent(icon?: string) {
  return workflowIconOptions.find(item => item.value === icon)?.component || Setting;
}

function isWorkflowPresetIcon(icon?: string) {
  return workflowIconOptions.some(item => item.value === icon);
}

function workflowIconLabel(icon?: string) {
  return workflowIconOptions.find(item => item.value === icon)?.label || '流程图标';
}

function workflowIconPreviewUrl(icon?: string) {
  if (!icon) {
    return '';
  }
  return isWorkflowPresetIcon(icon) ? '' : icon;
}

function loadWorkflowFormConfig(value?: string) {
  const config = parseWorkflowFormConfig(value);
  workflowFormMode.value = config.mode;
  workflowFormRules.value = config.rules;
  customFormFields.value = config.fields;
  Object.assign(customFormConfig, config.customConfig);
}

function parseWorkflowFormConfig(value?: string): { mode: WorkflowFormMode; rules: FcRule[]; fields: CustomFormField[]; customConfig: CustomFormConfig } {
  if (!value) {
    const rules = defaultWorkflowFormRules();
    return { mode: 'DYNAMIC', rules, fields: formCreateRulesToCustomFields(rules), customConfig: defaultCustomFormConfig() };
  }
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      const rules = normalizeFormCreateRules(parsed);
      return { mode: 'DYNAMIC', rules, fields: formCreateRulesToCustomFields(rules), customConfig: defaultCustomFormConfig() };
    }
    const mode: WorkflowFormMode = parsed?.mode === 'CUSTOM' ? 'CUSTOM' : 'DYNAMIC';
    const customConfig = normalizeCustomFormConfig(parsed?.customConfig || parsed);
    if (Array.isArray(parsed?.rules)) {
      const rules = normalizeFormCreateRules(parsed.rules);
      const fields = Array.isArray(parsed?.fields) ? normalizeCustomFormFieldsValue(parsed.fields) : formCreateRulesToCustomFields(rules);
      return { mode, rules, fields, customConfig };
    }
    if (Array.isArray(parsed?.fields)) {
      const fields = normalizeCustomFormFieldsValue(parsed.fields);
      const rules = customFieldsToFormCreateRules(fields);
      return { mode: 'CUSTOM', rules, fields, customConfig };
    }
    const rules = defaultWorkflowFormRules();
    return { mode: 'DYNAMIC', rules, fields: formCreateRulesToCustomFields(rules), customConfig: defaultCustomFormConfig() };
  } catch {
    const rules = defaultWorkflowFormRules();
    return { mode: 'DYNAMIC', rules, fields: formCreateRulesToCustomFields(rules), customConfig: defaultCustomFormConfig() };
  }
}

function stringifyWorkflowFormConfig() {
  return JSON.stringify({
    mode: workflowFormMode.value,
    rules: workflowFormRules.value,
    fields: workflowFormMode.value === 'CUSTOM' ? customFormFields.value : undefined,
    customConfig: workflowFormMode.value === 'CUSTOM' ? customFormConfig : undefined,
  }, null, 2);
}

function normalizeFieldKey(value: string) {
  const normalized = String(value || '')
    .trim()
    .replace(/[^A-Za-z0-9_]/g, '_')
    .replace(/^[^A-Za-z_]+/, '');
  return normalized || createFieldKey();
}

function createFieldKey(index = workflowFormRules.value.length + 1) {
  return `field_${index}`;
}

function syncWorkflowFormFromDesigner() {
  if (workflowFormMode.value !== 'DYNAMIC' || !formDesignerRef.value) {
    return;
  }
  workflowFormRules.value = normalizeFormCreateRules(formDesignerRef.value.getRule() || []);
}

function applyFormRulesToDesigner() {
  if (!formDesignerRef.value) {
    return;
  }
  formDesignerRef.value.setRule(workflowFormRules.value);
}

function syncCurrentWorkflowForm() {
  if (workflowFormMode.value === 'CUSTOM') {
    syncCustomWorkflowForm();
    return;
  }
  syncWorkflowFormFromDesigner();
}

async function handleWorkflowFormModeChange() {
  if (workflowFormMode.value === 'CUSTOM') {
    syncWorkflowFormFromDesigner();
    customFormFields.value = formCreateRulesToCustomFields(workflowFormRules.value);
    syncCustomWorkflowForm();
    return;
  }
  workflowFormRules.value = customFieldsToFormCreateRules(customFormFields.value);
  await nextTick();
  await registerWorkflowBusinessFormComponents();
  applyFormRulesToDesigner();
}

async function setWorkflowFormMode(mode: WorkflowFormMode) {
  if (workflowFormMode.value === mode) {
    return;
  }
  workflowFormMode.value = mode;
  await handleWorkflowFormModeChange();
}

function addCustomFormField() {
  customFormFields.value.push({
    key: createFieldKey(customFormFields.value.length + 1),
    label: `字段${customFormFields.value.length + 1}`,
    type: 'input',
    required: false,
    optionsText: '',
    placeholder: '',
    defaultValue: '',
  });
  syncCustomWorkflowForm();
}

function removeCustomFormField(index: number) {
  customFormFields.value.splice(index, 1);
  syncCustomWorkflowForm();
}

function normalizeCustomFormFields() {
  customFormFields.value = normalizeCustomFormFieldsValue(customFormFields.value);
  syncCustomWorkflowForm();
}

function syncCustomWorkflowForm() {
  customFormFields.value = normalizeCustomFormFieldsValue(customFormFields.value);
  workflowFormRules.value = customFieldsToFormCreateRules(customFormFields.value);
}

function validateWorkflowForm(silent: boolean) {
  syncCurrentWorkflowForm();
  if (workflowFormMode.value === 'CUSTOM') {
    const invalid = customFormFields.value.find(field => !field.key || !field.label);
    if (invalid) {
      if (!silent) {
        ElMessage.warning('请完善自定义表单字段标识和字段名称');
      }
      return false;
    }
  }
  return true;
}

function normalizeFormCreateRules(rules: any[]): FcRule[] {
  const normalized = (rules || [])
    .filter(Boolean)
    .map((rule, index) => normalizeFormCreateRule(rule, index));
  return normalized.length > 0 ? normalized : defaultWorkflowFormRules();
}

function normalizeFormCreateRule(rule: any, index: number): FcRule {
  const next = { ...rule };
  if (next.field) {
    next.field = normalizeFieldKey(next.field);
  } else if (isFormInputRule(next)) {
    next.field = createFieldKey(index + 1);
  }
  return next;
}

function legacyFieldsToFormCreateRules(fields: any[]): FcRule[] {
  const rules = fields
    .filter(Boolean)
    .map((field, index) => legacyFieldToFormCreateRule(field, index));
  return rules.length > 0 ? rules : defaultWorkflowFormRules();
}

function defaultCustomFormConfig(): CustomFormConfig {
  return { submitPath: '', viewPath: '' };
}

function normalizeCustomFormConfig(value: any): CustomFormConfig {
  return {
    submitPath: String(value?.submitPath || value?.createPath || ''),
    viewPath: String(value?.viewPath || value?.detailPath || ''),
  };
}

function normalizeCustomFormFieldsValue(fields: any[]): CustomFormField[] {
  return (fields || [])
    .filter(Boolean)
    .map((field, index) => {
      const key = normalizeFieldKey(field?.key || field?.field || createFieldKey(index + 1));
      const label = String(field?.label || field?.title || key);
      const props = field?.props || {};
      const type = normalizeCustomFieldType(props?.workflowDataType || field?.workflowDataType || field?.type);
      return {
        key,
        label,
        type,
        required: Boolean(field?.required || field?.validate?.some?.((rule: any) => rule?.required) || field?.rules?.some?.((rule: any) => rule?.required)),
        optionsText: type === 'select' ? field?.optionsText || optionsToText(field?.options) : '',
        placeholder: String(field?.placeholder || props?.placeholder || ''),
        defaultValue: field?.defaultValue === undefined || field?.defaultValue === null ? '' : String(field.defaultValue),
      };
    });
}

function normalizeCustomFieldType(type?: string) {
  if (type === 'number') return 'inputNumber';
  if (type === 'datetime') return 'datePicker';
  const normalized = String(type || '');
  if (['input', 'textarea', 'inputNumber', 'select', 'datePicker', 'systemUser', 'systemOrg', 'systemPost', 'systemRole'].includes(normalized)) {
    return normalized;
  }
  return 'input';
}

function customFieldsToFormCreateRules(fields: CustomFormField[]): FcRule[] {
  const rules = normalizeCustomFormFieldsValue(fields)
    .map((field): FcRule => {
      const formCreateType = customFieldToFormCreateType(field.type);
      const rule: FcRule = {
        type: formCreateType,
        field: field.key,
        title: field.label,
        props: {
          placeholder: field.placeholder || (isSelectLikeCustomFieldType(field.type) ? `请选择${field.label}` : `请输入${field.label}`),
          workflowDataType: isSystemCustomFieldType(field.type) ? field.type : undefined,
          clearable: isSelectLikeCustomFieldType(field.type) ? true : undefined,
          filterable: isSelectLikeCustomFieldType(field.type) ? true : undefined,
        },
        validate: field.required
          ? [{ required: true, message: `${field.label}不能为空`, trigger: field.type === 'input' || field.type === 'textarea' ? 'blur' : 'change' }]
          : [],
      };
      if (field.type === 'select') {
        rule.options = textToOptions(field.optionsText);
      }
      if (isSystemCustomFieldType(field.type)) {
        const options = customFieldSystemOptions(field.type).map(item => ({ label: item.label, value: item.value }));
        if (field.type === 'systemOrg') {
          rule.props = {
            ...rule.props,
            data: approvalOrgTreeOptions.value,
            nodeKey: 'value',
            checkStrictly: true,
          };
        } else {
          rule.options = options;
        }
      }
      if (field.defaultValue) {
        (rule as any).value = field.defaultValue;
      }
      return rule;
    });
  return rules.length > 0 ? rules : defaultWorkflowFormRules();
}

function formCreateRulesToCustomFields(rules: FcRule[]): CustomFormField[] {
  return collectWorkflowFormVariables(rules).map(variable => {
    const rule = findRuleByField(rules, variable.value) || {};
    const props = rule.props || {};
    return {
      key: variable.value,
      label: variable.label,
      type: normalizeCustomFieldType(String(props.workflowDataType || rule.type || 'input')),
      required: Array.isArray(rule.validate) && rule.validate.some((item: any) => item?.required),
      optionsText: optionsToText(rule.options),
      placeholder: String(props.placeholder || ''),
      defaultValue: rule.value === undefined || rule.value === null ? '' : String(rule.value),
    };
  });
}

function customFieldToFormCreateType(type: string) {
  if (type === 'systemOrg') {
    return 'elTreeSelect';
  }
  if (isSystemCustomFieldType(type)) {
    return 'select';
  }
  return type;
}

function isSelectLikeCustomFieldType(type: string) {
  return type === 'select' || isSystemCustomFieldType(type);
}

function isSystemCustomFieldType(type: string) {
  return ['systemUser', 'systemOrg', 'systemPost', 'systemRole'].includes(String(type));
}

function customFieldSystemLoading(type: string) {
  const key = customFieldSystemLoadingKey(type);
  return key ? approvalTargetLoading[key] : false;
}

function customFieldSystemOptions(type: string): ApprovalTargetOption[] {
  if (type === 'systemUser') return approvalUserOptions.value;
  if (type === 'systemOrg') return flattenApprovalOrgTree(approvalOrgTreeOptions.value);
  if (type === 'systemPost') return approvalPostOptions.value;
  if (type === 'systemRole') return approvalRoleOptions.value;
  return [];
}

async function ensureCustomFieldSystemOptions(type: string) {
  if (type === 'systemUser') {
    await ensureApprovalUsersLoaded();
  } else if (type === 'systemOrg') {
    await ensureApprovalOrgsLoaded();
  } else if (type === 'systemPost') {
    await ensureApprovalPostsLoaded();
  } else if (type === 'systemRole') {
    await ensureApprovalRolesLoaded();
  }
}

function customFieldSystemLoadingKey(type: string): keyof typeof approvalTargetLoading | undefined {
  if (type === 'systemUser') return 'users';
  if (type === 'systemOrg') return 'orgs';
  if (type === 'systemPost') return 'posts';
  if (type === 'systemRole') return 'roles';
  return undefined;
}

function updateCustomFieldDefault(field: CustomFormField, value: string) {
  field.defaultValue = value || '';
  syncCustomWorkflowForm();
}

function handleCustomFieldTypeChange(field: CustomFormField) {
  field.defaultValue = '';
  if (field.type !== 'select') {
    field.optionsText = '';
  }
  if (isSystemCustomFieldType(field.type)) {
    void ensureCustomFieldSystemOptions(field.type);
  }
  syncCustomWorkflowForm();
}

function flattenApprovalOrgTree(items: ApprovalOrgTreeOption[]): ApprovalTargetOption[] {
  const result: ApprovalTargetOption[] = [];
  const visit = (nodes: ApprovalOrgTreeOption[]) => {
    for (const node of nodes || []) {
      result.push({ value: String(node.value), label: String(node.label) });
      if (node.children?.length) {
        visit(node.children);
      }
    }
  };
  visit(items);
  return result;
}

function findRuleByField(rules: any[], field: string): any {
  for (const item of rules || []) {
    if (item?.field === field) {
      return item;
    }
    if (Array.isArray(item?.children)) {
      const matched = findRuleByField(item.children, field);
      if (matched) {
        return matched;
      }
    }
  }
  return undefined;
}

function optionsToText(options: any[] | undefined) {
  if (!Array.isArray(options)) {
    return '';
  }
  return options
    .map(option => `${option?.label || option?.value || ''}=${option?.value || option?.label || ''}`)
    .filter(Boolean)
    .join(',');
}

function textToOptions(value?: string) {
  return String(value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(item => {
      const [label, optionValue] = item.split('=').map(part => part.trim());
      return { label: label || optionValue, value: optionValue || label };
    });
}

function legacyFieldToFormCreateRule(field: any, index: number): FcRule {
  const key = normalizeFieldKey(field?.key || createFieldKey(index + 1));
  const title = field?.label || key;
  const placeholder = field?.placeholder || `请输入${title}`;
  const base: FcRule = {
    type: mapLegacyFieldType(field?.type),
    field: key,
    title,
    props: { placeholder },
    validate: normalizeLegacyRules(field?.rules, title),
  };
  if (Array.isArray(field?.options) && ['select', 'radio', 'checkbox'].includes(String(field?.type))) {
    base.options = field.options.map((option: any) => ({
      label: String(option?.label || option?.value || '选项'),
      value: String(option?.value || option?.label || ''),
    }));
  }
  return base;
}

function mapLegacyFieldType(type?: string) {
  if (type === 'number') return 'inputNumber';
  if (type === 'datetime') return 'datePicker';
  return type || 'input';
}

function normalizeLegacyRules(rules: any[] | undefined, title: string) {
  if (!Array.isArray(rules)) return [];
  return rules.map(rule => ({
    ...rule,
    message: rule?.message || `${title}不能为空`,
    trigger: rule?.trigger || 'change',
  }));
}

function collectWorkflowFormVariables(rules: FcRule[]): WorkflowVariableOption[] {
  const variables: WorkflowVariableOption[] = [];
  const visit = (items: any[]) => {
    for (const item of items || []) {
      if (isFormInputRule(item) && item.field) {
        variables.push({
          label: item.title || item.field,
          value: item.field,
          source: '表单字段',
          dataType: workflowFormRuleDataType(item),
        });
      }
      if (Array.isArray(item.children)) {
        visit(item.children);
      }
    }
  };
  visit(rules);
  return variables;
}

function workflowFormRuleDataType(rule: any): WorkflowVariableOption['dataType'] {
  const workflowDataType = String(rule?.props?.workflowDataType || '').trim();
  if (workflowDataType === 'systemUser') return 'USER';
  if (workflowDataType === 'systemOrg') return 'ORG';
  if (workflowDataType === 'systemPost') return 'POST';
  if (workflowDataType === 'systemRole') return 'ROLE';
  const type = String(rule?.type || '').trim();
  if (type === 'inputNumber') return 'NUMBER';
  if (type === 'datePicker' || type === 'timePicker' || type === 'timeRange' || type === 'dateRange') return 'DATE';
  return 'TEXT';
}

function normalizeWorkflowVariableDataType(type?: WorkflowVariableDataType) {
  const normalized = String(type || '').toUpperCase();
  if (['USER', 'ORG', 'POST', 'ROLE', 'NUMBER', 'DATE'].includes(normalized)) {
    return normalized as 'USER' | 'ORG' | 'POST' | 'ROLE' | 'NUMBER' | 'DATE';
  }
  return 'TEXT';
}

function findWorkflowVariable(variable: string) {
  for (const group of workflowVariableGroups.value) {
    const matched = group.options.find(item => item.value === variable);
    if (matched) {
      return matched;
    }
  }
  return undefined;
}

function workflowVariableDataType(variable: string) {
  return normalizeWorkflowVariableDataType(findWorkflowVariable(variable)?.dataType);
}

function isFormInputRule(rule: any) {
  return Boolean(rule?.field && rule.type !== 'hidden') || ['input', 'textarea', 'inputNumber', 'select', 'radio', 'checkbox', 'switch', 'datePicker', 'timePicker', 'cascader', 'treeSelect'].includes(String(rule?.type));
}

function updateNodeProperty(node: WorkflowDesignerNode, key: string, value: any) {
  node.properties ||= {};
  if (value === undefined || value === null || value === '') {
    delete node.properties[key];
  } else {
    node.properties[key] = value;
  }
  syncDesignerJson();
}

function defaultEventNotifyConfig(): WorkflowEventNotifyConfig {
  return {
    enabled: false,
    type: 'HTTP',
    method: 'POST',
    timeoutMillis: 5000,
  };
}

interface WorkflowRootNodeConfig {
  scopeType?: string;
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
  formPermissions?: Record<string, WorkflowFormPermission>;
}

interface WorkflowCcNodeConfig {
  targetTypes?: string[];
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
  eventName?: string;
  messageTemplate?: string;
}

function isRootNode(node: WorkflowDesignerNode) {
  return node.nodeType === 'ROOT' || node.bpmnType === 'startEvent';
}

function isCcNode(node: WorkflowDesignerNode) {
  return node.nodeType === 'CC';
}

function rootConfig(node: WorkflowDesignerNode): WorkflowRootNodeConfig {
  const current = node.properties?.rootConfig || {};
  return {
    scopeType: 'ALL',
    userIds: [],
    orgIds: [],
    roleIds: [],
    postIds: [],
    formPermissions: {},
    ...current,
  };
}

function updateRootConfig(node: WorkflowDesignerNode, patch: Partial<WorkflowRootNodeConfig>) {
  node.properties ||= {};
  node.properties.rootConfig = {
    ...rootConfig(node),
    ...patch,
  };
  syncDesignerJson();
}

function ccConfig(node: WorkflowDesignerNode): WorkflowCcNodeConfig {
  const current = node.properties?.ccConfig || {};
  return {
    targetTypes: [],
    userIds: [],
    orgIds: [],
    roleIds: [],
    postIds: [],
    eventName: node.properties?.eventName || 'workflow.cc',
    messageTemplate: '',
    ...current,
  };
}

function updateCcConfig(node: WorkflowDesignerNode, patch: WorkflowCcNodeConfig) {
  node.properties ||= {};
  const next = {
    ...ccConfig(node),
    ...patch,
  };
  node.properties.ccConfig = next;
  node.properties.eventName = next.eventName || 'workflow.cc';
  syncDesignerJson();
}

function reservedNodePropertyKeys(node: WorkflowDesignerNode) {
  const keys = ['approvalConfig', 'rootConfig', 'ccConfig', 'formPermissions', 'eventNotify', 'conditionEditMode'];
  if (isCcNode(node)) {
    keys.push('eventName');
  }
  return keys;
}

function advancedNodeProperties(node: WorkflowDesignerNode) {
  const reserved = new Set(reservedNodePropertyKeys(node));
  return Object.fromEntries(Object.entries(node.properties || {}).filter(([key]) => !reserved.has(key)));
}

function updateAdvancedNodeProperties(node: WorkflowDesignerNode, value: Record<string, any>) {
  node.properties ||= {};
  const reserved = new Set(reservedNodePropertyKeys(node));
  for (const key of Object.keys(node.properties)) {
    if (!reserved.has(key)) {
      delete node.properties[key];
    }
  }
  Object.assign(node.properties, value);
  syncDesignerJson();
}

function approvalConfig(node: WorkflowDesignerNode): WorkflowApprovalNodeConfig {
  const current = node.properties?.approvalConfig || {};
  return {
    ...defaultApprovalConfig(),
    ...current,
    eventNotify: {
      ...defaultApprovalConfig().eventNotify,
      ...(current.eventNotify || {}),
    },
    formPermissions: {
      ...(current.formPermissions || {}),
    },
  };
}

function nodeEventNotifyConfig(node: WorkflowDesignerNode): WorkflowEventNotifyConfig {
  if (isUserTaskNode(node)) {
    return {
      ...defaultEventNotifyConfig(),
      ...(approvalConfig(node).eventNotify || {}),
    };
  }
  return {
    ...defaultEventNotifyConfig(),
    ...(node.properties?.eventNotify || {}),
  };
}

function updateNodeEventNotifyConfig(node: WorkflowDesignerNode, patch: Partial<WorkflowEventNotifyConfig>) {
  if (isUserTaskNode(node)) {
    updateApprovalEventNotify(node, patch);
    return;
  }
  node.properties ||= {};
  node.properties.eventNotify = {
    ...nodeEventNotifyConfig(node),
    ...patch,
  };
  syncDesignerJson();
}

function updateApprovalConfig(node: WorkflowDesignerNode, patch: Partial<WorkflowApprovalNodeConfig>) {
  node.properties ||= {};
  const config = approvalConfig(node);
  node.properties.approvalConfig = {
    ...config,
    ...patch,
  };
  syncDesignerJson();
}

function updateApprovalAssigneeType(node: WorkflowDesignerNode, value: unknown) {
  updateApprovalConfig(node, { assigneeType: value as WorkflowApprovalNodeConfig['assigneeType'] });
  if (value === 'SPECIFIED_USER') {
    void ensureApprovalUsersLoaded();
  } else if (value === 'SPECIFIED_ROLE') {
    void ensureApprovalRolesLoaded();
  } else if (value === 'SPECIFIED_POST') {
    void ensureApprovalPostsLoaded();
  } else if (value === 'SPECIFIED_ORG' || value === 'ORG_LEADER') {
    void ensureApprovalOrgsLoaded();
  }
}

function updateEmptyAssigneeStrategy(node: WorkflowDesignerNode, value: unknown) {
  updateApprovalConfig(node, { emptyAssigneeStrategy: value as WorkflowApprovalNodeConfig['emptyAssigneeStrategy'] });
  if (value === 'TO_USER') {
    void ensureApprovalUsersLoaded();
  }
}

function showApprovalModeConfig(node: WorkflowDesignerNode) {
  const config = approvalConfig(node);
  if (config.assigneeType === 'SPECIFIED_USER') {
    return (config.assigneeIds || []).length > 1;
  }
  if (config.assigneeType === 'SPECIFIED_ROLE' || config.assigneeType === 'ORG_LEADER') {
    return true;
  }
  if (config.assigneeType === 'SPECIFIED_POST' || config.assigneeType === 'SPECIFIED_ORG') {
    return true;
  }
  if (config.assigneeType === 'INITIATOR_SELECT') {
    return Boolean(config.initiatorSelectMultiple);
  }
  return config.assigneeType === 'FORM_USER';
}

function updateApprovalEventNotify(node: WorkflowDesignerNode, patch: Partial<WorkflowEventNotifyConfig>) {
  node.properties ||= {};
  const config = approvalConfig(node);
  node.properties.approvalConfig = {
    ...config,
    eventNotify: {
      ...(config.eventNotify || {}),
      ...patch,
    },
  };
  syncDesignerJson();
}

function updateApprovalListValue(node: WorkflowDesignerNode, key: keyof WorkflowApprovalNodeConfig, value: unknown) {
  updateApprovalConfig(node, {
    [key]: normalizeApprovalIds(value),
  } as Partial<WorkflowApprovalNodeConfig>);
}

function normalizeApprovalIds(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean);
  }
  if (value === undefined || value === null || value === '') {
    return [];
  }
  return [String(value).trim()].filter(Boolean);
}

async function ensureApprovalUsersLoaded() {
  if (!approvalTargetLoaded.users) {
    await searchApprovalUsers();
  }
}

async function searchApprovalUsers(keyword = '') {
  approvalTargetLoading.users = true;
  try {
    const data = await get<BackendPageResult<any>>('/identity/users/page', {
      params: {
        page: 1,
        size: 100,
      },
    });
    approvalUserOptions.value = filterApprovalTargets(toPageList(data), keyword, ['username', 'nickname', 'memberName'])
      .map(item => {
        const id = item.userId ?? item.id ?? item.memberId;
        const value = item.username ?? id;
        const name = item.nickname || item.memberName || item.username || id;
        const username = item.username && item.username !== name ? ` / ${item.username}` : '';
        return id === undefined ? undefined : { value: String(value), label: `${name}${username}` };
      })
      .filter(Boolean) as ApprovalTargetOption[];
    approvalTargetLoaded.users = true;
  } finally {
    approvalTargetLoading.users = false;
  }
}

async function ensureApprovalRolesLoaded() {
  if (!approvalTargetLoaded.roles) {
    await loadApprovalRoles();
  }
}

async function loadApprovalRoles() {
  approvalTargetLoading.roles = true;
  try {
    const data = await get<any[]>('/authorization/roles');
    approvalRoleOptions.value = (data || [])
      .map(item => {
        const id = item.roleId ?? item.id;
        const name = item.roleName || item.roleCode || id;
        const code = item.roleCode && item.roleCode !== name ? ` / ${item.roleCode}` : '';
        return id === undefined ? undefined : { value: String(id), label: `${name}${code}` };
      })
      .filter(Boolean) as ApprovalTargetOption[];
    approvalTargetLoaded.roles = true;
  } finally {
    approvalTargetLoading.roles = false;
  }
}

async function ensureApprovalPostsLoaded() {
  if (!approvalTargetLoaded.posts) {
    await searchApprovalPosts();
  }
}

async function searchApprovalPosts(keyword = '') {
  approvalTargetLoading.posts = true;
  try {
    const data = await get<BackendPageResult<any>>('/post/page', {
      params: {
        page: 1,
        size: 100,
      },
    });
    approvalPostOptions.value = filterApprovalTargets(toPageList(data), keyword, ['postName', 'postCode'])
      .map(item => {
        const id = item.id ?? item.postId;
        const name = item.postName || item.postCode || id;
        const code = item.postCode && item.postCode !== name ? ` / ${item.postCode}` : '';
        return id === undefined ? undefined : { value: String(id), label: `${name}${code}` };
      })
      .filter(Boolean) as ApprovalTargetOption[];
    approvalTargetLoaded.posts = true;
  } finally {
    approvalTargetLoading.posts = false;
  }
}

async function ensureApprovalOrgsLoaded() {
  if (!approvalTargetLoaded.orgs) {
    await loadApprovalOrgs();
  }
}

async function loadApprovalOrgs() {
  approvalTargetLoading.orgs = true;
  try {
    const data = await get<any[]>('/org/tree', { params: { parentId: 0, includeDisabled: true } });
    approvalOrgTreeOptions.value = toApprovalOrgTree(data || []);
    approvalTargetLoaded.orgs = true;
  } finally {
    approvalTargetLoading.orgs = false;
  }
}

function toPageList<T = any>(data?: BackendPageResult<T>): T[] {
  return data?.records || data?.list || [];
}

function filterApprovalTargets<T extends Record<string, any>>(items: T[], keyword: string, keys: string[]) {
  const normalized = String(keyword || '').trim().toLowerCase();
  if (!normalized) {
    return items;
  }
  return items.filter(item => keys.some(key => String(item[key] || '').toLowerCase().includes(normalized)));
}

function toApprovalOrgTree(items: any[]): ApprovalOrgTreeOption[] {
  return (items || [])
    .map(item => {
      const id = item.id ?? item.orgId;
      const name = item.orgName || item.name || item.orgCode || id;
      if (id === undefined) {
        return undefined;
      }
      return {
        value: String(id),
        label: String(name),
        children: item.children?.length ? toApprovalOrgTree(item.children) : undefined,
      };
    })
    .filter(Boolean) as ApprovalOrgTreeOption[];
}

function nodeFieldPermission(node: WorkflowDesignerNode, field: string): WorkflowFormPermission {
  if (isRootNode(node)) {
    return rootConfig(node).formPermissions?.[field] || 'EDITABLE';
  }
  if (isUserTaskNode(node)) {
    return approvalConfig(node).formPermissions?.[field] || 'READONLY';
  }
  return node.properties?.formPermissions?.[field] || 'READONLY';
}

function updateNodeFieldPermission(node: WorkflowDesignerNode, field: string, permission: WorkflowFormPermission) {
  if (isRootNode(node)) {
    const config = rootConfig(node);
    updateRootConfig(node, {
      formPermissions: {
        ...(config.formPermissions || {}),
        [field]: permission,
      },
    });
    return;
  }
  const config = approvalConfig(node);
  if (isUserTaskNode(node)) {
    updateApprovalConfig(node, {
      formPermissions: {
        ...(config.formPermissions || {}),
        [field]: permission,
      },
    });
    return;
  }
  node.properties ||= {};
  node.properties.formPermissions = {
    ...(node.properties.formPermissions || {}),
    [field]: permission,
  };
  syncDesignerJson();
}

function isUserTaskNode(node: WorkflowDesignerNode) {
  if (node.nodeType === 'ROOT' || node.bpmnType === 'startEvent') {
    return false;
  }
  return node.bpmnType === 'userTask' || node.executionType === 'USER_TASK' || node.nodeType === 'APPROVAL' || node.nodeType?.startsWith('GUARANTEE_');
}

function isServiceTaskNode(node: WorkflowDesignerNode) {
  return node.bpmnType === 'serviceTask' || node.nodeType === 'SERVICE' || ['SPRING_BEAN', 'HTTP_URL', 'REMOTE_SERVICE', 'EVENT_PUBLISH'].includes(node.executionType || '');
}

function hasAdvancedNodeProperties(node: WorkflowDesignerNode) {
  if (node.nodeType === 'EXCLUSIVE_BRANCH') {
    return false;
  }
  return Object.keys(node.properties || {}).length > 0;
}

function hasNodeSpecificConfig(node: WorkflowDesignerNode) {
  return isRootNode(node) || node.nodeType === 'EXCLUSIVE_BRANCH' || isUserTaskNode(node) || isCcNode(node) || isServiceTaskNode(node) || hasAdvancedNodeProperties(node);
}

function conditionEditMode(node: WorkflowDesignerNode): ConditionEditMode {
  return node.properties?.conditionEditMode === 'EXPRESSION' ? 'EXPRESSION' : 'BUILDER';
}

function updateConditionEditMode(node: WorkflowDesignerNode, mode: ConditionEditMode) {
  node.properties ||= {};
  node.properties.conditionEditMode = mode;
  if (mode === 'BUILDER') {
    parseConditionToBuilder(node.conditionExpression || '');
    applyConditionBuilder();
  }
  syncDesignerJson();
}

function createConditionRow(partial: Partial<ConditionRow> = {}): ConditionRow {
  const operator = partial.operator || '==';
  const shouldValidateOperator = Boolean(partial.variable) && isWorkflowVariableGroupsReady();
  return {
    id: createNodeId('condition'),
    connector: partial.connector || 'AND',
    variable: partial.variable || '',
    operator: shouldValidateOperator && !validConditionOperator(partial.variable || '', operator) ? defaultConditionOperator(partial.variable || '') : operator,
    value: partial.value ?? '',
  };
}

function addConditionRow(groupIndex?: number) {
  const targetIndex = typeof groupIndex === 'number' ? groupIndex : conditionGroups.value.length - 1;
  const targetGroup = conditionGroups.value[targetIndex];
  if (!targetGroup) {
    conditionGroups.value = [createConditionGroup()];
    return;
  }
  targetGroup.rows.push(createConditionRow());
  applyConditionBuilder();
}

function createConditionGroup(partial: Partial<ConditionGroup> = {}): ConditionGroup {
  return {
    id: createNodeId('condition-group'),
    connector: partial.connector || 'AND',
    rows: partial.rows?.length ? partial.rows : [createConditionRow()],
  };
}

function addConditionGroup() {
  conditionGroups.value.push(createConditionGroup({ connector: 'AND' }));
  applyConditionBuilder();
}

function removeConditionGroup(index: number) {
  if (conditionGroups.value.length <= 1) {
    return;
  }
  conditionGroups.value.splice(index, 1);
  applyConditionBuilder();
}

function removeConditionRow(groupIndex: number, rowIndex: number) {
  const group = conditionGroups.value[groupIndex];
  if (!group || group.rows.length <= 1) {
    return;
  }
  group.rows.splice(rowIndex, 1);
  applyConditionBuilder();
}

function applyConditionBuilder() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'EXCLUSIVE_BRANCH') {
    return;
  }
  const groupParts = conditionGroups.value
    .map((group, groupIndex) => {
      const rows = group.rows
        .filter(row => row.variable)
        .map((row, rowIndex) => {
          const expression = formatConditionExpression(row);
          if (rowIndex === 0) {
            return expression;
          }
          return `${row.connector === 'OR' ? '||' : '&&'} ${expression}`;
        })
        .filter(Boolean);
      if (rows.length === 0) {
        return undefined;
      }
      return {
        connector: groupIndex === 0 ? 'AND' : group.connector,
        expression: `(${rows.join(' ')})`,
      };
    })
    .filter(Boolean) as Array<{ connector: 'AND' | 'OR'; expression: string }>;
  if (groupParts.length === 0) {
    selectedNode.value.conditionExpression = '';
    syncDesignerJson();
    return;
  }
  selectedNode.value.conditionExpression = `\${${groupParts.map((groupPart, index) => index === 0 ? groupPart.expression : `${groupPart.connector === 'OR' ? '||' : '&&'} ${groupPart.expression}`).join(' ')}}`;
  syncDesignerJson();
}

function conditionOperatorOptions(variable: string) {
  const dataType = workflowVariableDataType(variable);
  if (dataType === 'NUMBER' || dataType === 'DATE') {
    return ['==', '!=', '>', '>=', '<', '<='];
  }
  return ['==', '!=', 'contains', 'notContains'];
}

function isWorkflowVariableGroupsReady() {
  try {
    return Array.isArray(workflowVariableGroups.value);
  } catch {
    return false;
  }
}

function defaultConditionOperator(variable: string) {
  return conditionOperatorOptions(variable)[0] || '==';
}

function validConditionOperator(variable: string, operator: string) {
  return conditionOperatorOptions(variable).includes(operator);
}

function parseConditionToBuilder(expression: string) {
  const inner = expression.match(/^\$\{\s*(.+?)\s*}$/)?.[1] || '';
  if (!inner) {
    conditionGroups.value = [createConditionGroup()];
    return;
  }
  const groups = splitConditionTerms(inner).map((groupPart, groupIndex) => {
    const groupText = stripConditionWrapper(groupPart.text);
    const rows = splitConditionTerms(groupText).map((rowPart, rowIndex) => {
      const rowText = stripConditionWrapper(rowPart.text);
      const matched = rowText.match(/^([A-Za-z_][\w.]*)\s*(==|!=|>=|<=|>|<)\s*('(?:\\'|[^'])*'|true|false|null|-?\d+(?:\.\d+)?|[^\s&|]+)/);
      if (!matched) {
        return undefined;
      }
      return createConditionRow({
        connector: rowIndex === 0 ? 'AND' : rowPart.connector,
        variable: matched[1],
        operator: matched[2],
        value: unformatConditionValue(matched[3]),
      });
    }).filter(Boolean) as ConditionRow[];
    return createConditionGroup({
      connector: groupIndex === 0 ? 'AND' : groupPart.connector,
      rows: rows.length > 0 ? rows : [createConditionRow()],
    });
  });
  conditionGroups.value = groups.length > 0 ? groups : [createConditionGroup()];
}

function splitConditionTerms(expression: string) {
  const parts: Array<{ connector: 'AND' | 'OR'; text: string }> = [];
  let buffer = '';
  let depth = 0;
  let quote: string | undefined;
  let pendingConnector: 'AND' | 'OR' = 'AND';
  for (let i = 0; i < expression.length; i += 1) {
    const char = expression[i];
    const next = expression[i + 1];
    if (quote) {
      buffer += char;
      if (char === '\\') {
        buffer += next || '';
        i += next ? 1 : 0;
        continue;
      }
      if (char === quote) {
        quote = undefined;
      }
      continue;
    }
    if (char === '\'' || char === '"') {
      quote = char;
      buffer += char;
      continue;
    }
    if (char === '(') {
      depth += 1;
      buffer += char;
      continue;
    }
    if (char === ')') {
      depth = Math.max(0, depth - 1);
      buffer += char;
      continue;
    }
    if (depth === 0 && char === '&' && next === '&') {
      if (buffer.trim()) {
        parts.push({ connector: pendingConnector, text: buffer.trim() });
      }
      pendingConnector = 'AND';
      buffer = '';
      i += 1;
      continue;
    }
    if (depth === 0 && char === '|' && next === '|') {
      if (buffer.trim()) {
        parts.push({ connector: pendingConnector, text: buffer.trim() });
      }
      pendingConnector = 'OR';
      buffer = '';
      i += 1;
      continue;
    }
    buffer += char;
  }
  if (buffer.trim()) {
    parts.push({ connector: pendingConnector, text: buffer.trim() });
  }
  return parts.length > 0 ? parts : [{ connector: 'AND', text: expression.trim() }];
}

function stripConditionWrapper(value: string) {
  const trimmed = String(value || '').trim();
  if (!trimmed.startsWith('(') || !trimmed.endsWith(')')) {
    return trimmed;
  }
  let depth = 0;
  let quote: string | undefined;
  for (let i = 0; i < trimmed.length; i += 1) {
    const char = trimmed[i];
    const next = trimmed[i + 1];
    if (quote) {
      if (char === '\\') {
        i += next ? 1 : 0;
        continue;
      }
      if (char === quote) {
        quote = undefined;
      }
      continue;
    }
    if (char === '\'' || char === '"') {
      quote = char;
      continue;
    }
    if (char === '(') {
      depth += 1;
    }
    if (char === ')') {
      depth -= 1;
      if (depth === 0 && i < trimmed.length - 1) {
        return trimmed;
      }
    }
  }
  return depth === 0 ? trimmed.slice(1, -1).trim() : trimmed;
}

function formatConditionValue(value: string | number, dataType: string) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) return "''";
  if (dataType === 'NUMBER') {
    return /^-?\d+(\.\d+)?$/.test(trimmed) ? trimmed : "''";
  }
  if (/^(true|false|null)$/i.test(trimmed)) {
    return trimmed;
  }
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed;
  }
  return `'${trimmed.replace(/'/g, "\\'")}'`;
}

function formatConditionExpression(row: ConditionRow) {
  const dataType = workflowVariableDataType(row.variable);
  const operator = validConditionOperator(row.variable, row.operator) ? row.operator : defaultConditionOperator(row.variable);
  if (operator === 'contains') {
    return `${row.variable}.contains(${formatConditionValue(row.value ?? '', dataType)})`;
  }
  if (operator === 'notContains') {
    return `!${row.variable}.contains(${formatConditionValue(row.value ?? '', dataType)})`;
  }
  return `${row.variable} ${operator} ${formatConditionValue(row.value ?? '', dataType)}`;
}

function unformatConditionValue(value: string) {
  const trimmed = String(value || '').trim();
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1).replace(/\\'/g, "'");
  }
  return trimmed;
}

function validateDesignerTree() {
  const errors: string[] = [];
  syncDesignerJson();
  if (!designerRoot.value.childNode) {
    errors.push('请至少配置一个流程节点');
  }
  collectNodeErrors(designerRoot.value, errors);
  return errors;
}

function collectNodeErrors(node: WorkflowDesignerNode | null | undefined, errors: string[], requireCondition = true) {
  if (!node) return;
  if (!node.nodeName?.trim()) {
    errors.push('存在未命名的流程节点');
  }
  if (node.nodeType === 'EXCLUSIVE_BRANCH' && requireCondition && !node.conditionExpression?.trim()) {
    errors.push(`${node.nodeName || '条件分支'} 未配置条件表达式`);
  }
  if ((node.nodeType === 'EXCLUSIVE_GATEWAY' || node.nodeType === 'PARALLEL_GATEWAY') && (node.conditionNodes?.length || 0) < 2) {
    errors.push(`${node.nodeName || '网关节点'} 至少需要两个分支`);
  }
  if (isRootNode(node)) {
    collectRootConfigErrors(node, errors);
  }
  if (isUserTaskNode(node)) {
    collectApprovalConfigErrors(node, errors);
  }
  if (isCcNode(node)) {
    collectCcConfigErrors(node, errors);
  }
  const branches = node.conditionNodes || [];
  for (let index = 0; index < branches.length; index += 1) {
    const branch = branches[index];
    const shouldRequireCondition = node.nodeType === 'EXCLUSIVE_GATEWAY' && index < branches.length - 1;
    collectNodeErrors(branch, errors, shouldRequireCondition);
  }
  collectNodeErrors(node.childNode, errors);
}

function collectApprovalConfigErrors(node: WorkflowDesignerNode, errors: string[]) {
  const config = approvalConfig(node);
  const nodeName = node.nodeName || '审批节点';
  if (config.assigneeType === 'SPECIFIED_USER' && !(config.assigneeIds || []).length) {
    errors.push(`${nodeName} 未选择指定成员`);
  }
  if (config.assigneeType === 'SPECIFIED_ROLE' && !(config.roleIds || []).length) {
    errors.push(`${nodeName} 未选择角色`);
  }
  if (config.assigneeType === 'SPECIFIED_POST' && !(config.postIds || []).length) {
    errors.push(`${nodeName} 未选择岗位`);
  }
  if (config.assigneeType === 'SPECIFIED_ORG' && !(config.orgIds || []).length) {
    errors.push(`${nodeName} 未选择组织`);
  }
  if (config.assigneeType === 'ORG_LEADER' && config.orgLeaderUseInitiatorOrg === false && !(config.orgIds || []).length) {
    errors.push(`${nodeName} 未选择主管所在组织`);
  }
  if (config.assigneeType === 'FORM_USER' && !config.formUserField) {
    errors.push(`${nodeName} 未选择表单人员字段`);
  }
  if (config.assigneeType === 'EXPRESSION' && !config.expression?.trim()) {
    errors.push(`${nodeName} 未填写流程表达式`);
  }
  if (config.emptyAssigneeStrategy === 'TO_USER' && !(config.emptyAssigneeUserIds || []).length) {
    errors.push(`${nodeName} 审批人为空时未选择指定人员`);
  }
}

function collectRootConfigErrors(node: WorkflowDesignerNode, errors: string[]) {
  const config = rootConfig(node);
  const nodeName = node.nodeName || '发起人';
  const hasCompositeTargets = Boolean((config.userIds || []).length || (config.orgIds || []).length || (config.roleIds || []).length || (config.postIds || []).length);
  if (config.scopeType === 'SPECIFIED_USER' && !(config.userIds || []).length) {
    errors.push(`${nodeName} 未选择可发起成员`);
  }
  if (config.scopeType === 'SPECIFIED_ORG' && !(config.orgIds || []).length) {
    errors.push(`${nodeName} 未选择可发起组织`);
  }
  if (config.scopeType === 'COMPOSITE' && !hasCompositeTargets) {
    errors.push(`${nodeName} 未选择可发起对象`);
  }
}

function collectCcConfigErrors(node: WorkflowDesignerNode, errors: string[]) {
  const config = ccConfig(node);
  const nodeName = node.nodeName || '抄送节点';
  const hasTargets = Boolean((config.userIds || []).length || (config.orgIds || []).length || (config.roleIds || []).length || (config.postIds || []).length);
  if (!hasTargets) {
    errors.push(`${nodeName} 未选择抄送对象`);
  }
}

</script>

<style scoped>
.workflow-page {
  padding: 0;
}

.workflow-tabs {
  overflow: hidden;
  border: 1px solid var(--mango-border-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.workflow-tabs :deep(.el-tabs__header) {
  padding: 0 16px;
  margin: 0;
}

.workflow-tabs :deep(.el-tabs__nav-wrap::after) {
  left: -16px;
  right: -16px;
}

.workflow-tabs :deep(.el-tabs__content) {
  padding: 16px;
}

.workflow-definition-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.workflow-builder {
  min-height: calc(100vh - 132px);
  margin: -8px -8px 0;
  background: var(--el-bg-color);
}

.builder-header {
  position: sticky;
  top: 0;
  z-index: 5;
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) 240px;
  align-items: center;
  gap: 18px;
  padding: 14px 20px 10px;
  border-bottom: 0;
  background: var(--el-bg-color);
}

.builder-back {
  justify-self: start;
}

.builder-steps {
  display: flex;
  justify-content: center;
  gap: 8px;
  min-width: 0;
}

.builder-step {
  display: flex;
  width: max-content;
  align-items: center;
  gap: 10px;
  min-width: 156px;
  padding: 10px 14px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.builder-step.active {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.builder-step.done {
  color: var(--el-text-color-primary);
}

.builder-step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-weight: 700;
}

.builder-step.active .builder-step-index {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: #fff;
}

.builder-step-text {
  text-align: left;
  line-height: 1.15;
}

.builder-step-text strong {
  font-size: 18px;
}

.builder-step-text em {
  display: none;
}

.builder-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.builder-content {
  padding: 18px 28px 28px;
}

.builder-pane {
  min-height: calc(100vh - 252px);
}

.basic-pane,
.form-pane {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.basic-pane {
  align-items: center;
}

.pane-help {
  width: min(640px, 100%);
  padding-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.builder-form {
  max-width: none;
  padding: 0;
}

.builder-footer {
  position: sticky;
  bottom: 0;
  z-index: 4;
  display: flex;
  justify-content: center;
  gap: 10px;
  padding: 12px 20px;
  border-top: 0;
  background: rgba(255, 255, 255, 0.94);
}

.page-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.query-form {
  flex: 1;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.step-form,
.designer-workbench {
  border: 0;
  border-radius: 0;
  background: var(--el-bg-color);
}

.step-form {
  max-width: 760px;
  padding: 18px 16px 6px;
}

.basic-pane .step-form {
  display: grid;
  width: min(560px, 100%);
  max-width: 560px;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  padding: 24px 24px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.basic-pane .basic-field {
  margin-bottom: 0;
}

.basic-pane .basic-field.full {
  grid-column: 1 / -1;
}

.basic-pane .el-select {
  width: 100%;
}

.workflow-icon-field {
  display: flex;
  width: 100%;
  justify-content: center;
}

.workflow-icon-item :deep(.el-form-item__content) {
  display: flex;
  justify-content: center;
}

.workflow-icon-uploader {
  position: relative;
  width: 64px;
  height: 64px;
}

.workflow-icon-uploader :deep(.image-upload),
.workflow-icon-uploader :deep(.avatar-uploader) {
  width: 64px;
}

.workflow-icon-uploader :deep(.el-upload),
.workflow-icon-uploader :deep(.image-upload__card),
.workflow-icon-uploader :deep(.el-upload--picture-card),
.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 64px;
  height: 64px;
  border-radius: 10px;
}

.workflow-icon-uploader :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.workflow-icon-uploader.empty .workflow-icon-upload-control :deep(.el-upload--picture-card) {
  opacity: 0;
}

.workflow-icon-uploader :deep(.el-upload--picture-card:hover) {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-fill-color-light);
}

.workflow-icon-uploader.filled .workflow-icon-upload-control :deep(.el-upload--picture-card) {
  display: none;
}

.workflow-icon-uploader :deep(.el-upload-list--picture-card) {
  display: block;
}

.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  margin: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
}

.workflow-icon-uploader :deep(.el-upload-list__item-status-label) {
  display: none;
}

.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item-actions) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 10px;
}

.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item-actions span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: auto;
  height: auto;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item-actions .el-upload-list__item-preview),
.workflow-icon-uploader :deep(.el-upload-list--picture-card .el-upload-list__item-actions .el-upload-list__item-delete) {
  margin: 0;
  color: #fff;
  font-size: 18px;
  line-height: 1;
}

.workflow-icon-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef3f8 100%);
}

.workflow-icon-empty-state {
  color: var(--el-text-color-secondary);
  transition: all 0.2s ease;
  gap: 8px;
}

.workflow-icon-uploader.empty:hover .workflow-icon-empty-state {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.workflow-icon-empty-state .el-icon,
.workflow-icon-legacy-preview .el-icon {
  font-size: 18px;
}

.workflow-icon-empty-state span,
.workflow-icon-legacy-preview span {
  font-size: 12px;
  font-weight: 600;
}

.workflow-icon-legacy-preview {
  color: var(--el-color-primary);
}

.field-label-with-help {
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-label-with-help .el-icon {
  color: var(--el-text-color-secondary);
  cursor: help;
}

.form-step-layout {
  min-width: 0;
}

.form-designer-main {
  min-width: 0;
}

.form-design-shell {
  width: min(1180px, 100%);
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: linear-gradient(180deg, #fafcff 0%, var(--el-bg-color) 72px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.form-config-bar {
  padding: 16px 18px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: rgba(255, 255, 255, 0.82);
}

.form-config-main {
  display: grid;
  grid-template-columns: minmax(260px, 420px) minmax(0, 1fr);
  align-items: end;
  gap: 18px;
}

.form-code-form {
  min-width: 0;
}

.form-code-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.form-type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
}

.form-type-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 600;
}

.form-designer-summary {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.form-designer-summary strong {
  color: var(--el-color-primary);
  font-size: 22px;
}

.dynamic-designer-wrap,
.custom-form-builder {
  padding: 16px 18px 18px;
}

.dynamic-designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.dynamic-designer-title {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.dynamic-designer-title strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.dynamic-designer-title span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.dynamic-designer-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.workflow-form-designer {
  min-height: 560px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.dynamic-designer-wrap.hide-left :deep(._fc-l-menu),
.dynamic-designer-wrap.hide-left :deep(._fc-l) {
  display: none;
}

.dynamic-designer-wrap.hide-right :deep(._fc-r) {
  display: none;
}

.dynamic-designer-wrap :deep(._fc-m) {
  min-width: 0;
}

.custom-form-builder {
  display: grid;
  gap: 16px;
  background: transparent;
}

.custom-route-section {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.custom-route-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}

.custom-route-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.custom-form-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.custom-field-section {
  display: grid;
  gap: 12px;
}

.custom-field-list {
  display: grid;
  gap: 12px;
}

.custom-field-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.custom-field-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 700;
}

.custom-field-main {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.1fr) minmax(0, 1fr) 128px;
  gap: 12px;
}

.custom-field-main :deep(.el-form-item) {
  margin-bottom: 0;
}

.custom-field-main :deep(.el-select),
.custom-field-extra :deep(.el-select) {
  width: 100%;
}

.custom-field-extra {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.custom-field-extra label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 700;
}

.designer-workbench {
  min-width: 0;
  overflow: hidden;
}

.designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: 16px;
  position: relative;
  min-height: calc(100vh - 260px);
  padding: 0;
  background: transparent;
}

.designer-body.has-node-panel {
  grid-template-columns: minmax(0, 1fr) 380px;
}

:deep(.workflow-node-drawer) {
  min-width: 560px;
}

:deep(.workflow-node-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 22px 14px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

:deep(.workflow-node-drawer .el-drawer__body) {
  padding: 0 22px 22px;
}

.drawer-form {
  padding: 4px 0;
}

.compact-node-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.node-property-tabs {
  display: flex;
  min-height: 100%;
  flex-direction: column;
}

.node-property-tabs :deep(.el-tabs__header) {
  margin: -4px 0 14px;
}

.node-property-tabs :deep(.el-tabs__item) {
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 700;
}

.node-property-tabs :deep(.el-tabs__content) {
  flex: 1;
}

.form-permission-panel {
  display: grid;
  gap: 8px;
}

.form-permission-head,
.form-permission-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.form-permission-head {
  padding: 0 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

.form-permission-list {
  display: grid;
  gap: 8px;
}

.form-permission-row {
  min-height: 48px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.form-permission-field {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.form-permission-field strong,
.form-permission-field small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-permission-field strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.form-permission-field small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.variable-reference-panel {
  display: grid;
  gap: 14px;
  padding-bottom: 8px;
}

.variable-reference-group {
  display: grid;
  gap: 8px;
}

.variable-reference-source {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.variable-reference-list {
  display: grid;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.variable-reference-item {
  display: grid;
  gap: 4px;
  padding: 11px 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.variable-reference-item:first-child {
  border-top: 0;
}

.variable-reference-item strong,
.variable-reference-item em {
  overflow-wrap: anywhere;
  white-space: normal;
}

.variable-reference-item strong {
  color: var(--el-text-color-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}

.variable-reference-item em {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-style: normal;
}

.node-meta-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 2px;
}

.catalog-group {
  padding: 4px 0;
}

.catalog-group-title {
  padding: 4px 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.color-swatch {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  vertical-align: middle;
}

.xml-collapse {
  margin-top: 14px;
}

.validate-result {
  min-height: 220px;
  padding-top: 18px;
}

.validate-error {
  margin-top: 6px;
  color: var(--el-color-danger);
}

.validate-loading {
  display: inline-block;
  width: 72px;
  height: 72px;
}

.code-editor-form-item :deep(.el-form-item__content) {
  display: block;
  width: 100%;
}

.code-editor-form-item :deep(.code-editor-container),
.code-editor-form-item :deep(.cm-editor),
.code-editor-form-item :deep(.CodeMirror) {
  width: 100%;
}

:deep(.el-select) {
  min-width: 160px;
}

@media (max-width: 1024px) {
  .form-config-main,
  .custom-field-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-type-row,
  .dynamic-designer-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .custom-field-extra {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 1180px) {
  .builder-header {
    grid-template-columns: 1fr;
  }

  .builder-actions,
  .builder-back {
    justify-self: center;
  }

  .basic-pane,
  .form-pane,
  .form-step-layout,
  .designer-body {
    grid-template-columns: 1fr;
  }

  .basic-pane .step-form {
    grid-template-columns: 1fr;
    padding: 18px;
  }
}
</style>
