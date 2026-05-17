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
                <FcDesigner
                  ref="formDesignerRef"
                  :config="formDesignerConfig"
                  :menu="formDesignerMenu"
                  class="workflow-form-designer"
                  height="calc(100vh - 350px)"
                  @save="syncWorkflowFormFromDesigner"
              />
              </template>
              <div v-else class="custom-form-builder">
                <div class="custom-route-section">
                  <div class="section-title">自定义表单配置</div>
                  <el-form label-position="top">
                    <el-row :gutter="14">
                      <el-col :span="12">
                        <el-form-item label="表单提交路径">
                          <el-input v-model="customFormConfig.submitPath" placeholder="例如：/flow/guarantee/create" @input="syncCustomWorkflowForm" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="表单查看路径">
                          <el-input v-model="customFormConfig.viewPath" placeholder="例如：/flow/guarantee/detail" @input="syncCustomWorkflowForm" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                </div>
                <div class="custom-form-toolbar">
                  <el-button :icon="Plus" type="primary" @click="addCustomFormField">新增字段</el-button>
                  <span>字段会转换为流程变量，可在条件分支中选择。</span>
                </div>
                <el-table :data="customFormFields" border>
                  <el-table-column label="字段标识" min-width="150">
                    <template #default="{ row }">
                      <el-input v-model="row.key" placeholder="如 amount" @blur="normalizeCustomFormFields" />
                    </template>
                  </el-table-column>
                  <el-table-column label="字段名称" min-width="150">
                    <template #default="{ row }">
                      <el-input v-model="row.label" placeholder="如 保函金额" @input="syncCustomWorkflowForm" />
                    </template>
                  </el-table-column>
                  <el-table-column label="字段类型" width="150">
                    <template #default="{ row }">
                      <el-select v-model="row.type" @change="syncCustomWorkflowForm">
                        <el-option label="单行文本" value="input" />
                        <el-option label="多行文本" value="textarea" />
                        <el-option label="数字" value="inputNumber" />
                        <el-option label="下拉选择" value="select" />
                        <el-option label="日期" value="datePicker" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="必填" width="90" align="center">
                    <template #default="{ row }">
                      <el-switch v-model="row.required" @change="syncCustomWorkflowForm" />
                    </template>
                  </el-table-column>
                  <el-table-column label="选项" min-width="180">
                    <template #default="{ row }">
                      <el-input
                        v-model="row.optionsText"
                        :disabled="row.type !== 'select'"
                        placeholder="标准流程=STANDARD,特殊流程=SPECIAL"
                        @input="syncCustomWorkflowForm"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="90" align="center">
                    <template #default="{ $index }">
                      <el-button link type="danger" @click="removeCustomFormField($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
          <div class="pane-help">
            动态表单适合直接拖拽设计页面；自定义表单适合已有业务表单页面，只在这里维护流程变量字段。
          </div>
        </section>

        <section v-show="definitionStep === 2" class="builder-pane process-pane">
          <div class="designer-workbench">
            <div class="designer-body">
              <div class="node-canvas">
                <div class="node-canvas-scale" :style="{ transform: `scale(${canvasZoom})` }">
                  <workflow-node
                    :node="designerRoot"
                    :catalog="nodeCatalog"
                    root
                    @select="selectNode"
                    @changed="syncDesignerJson"
                  />
                  <div class="end-node">结束</div>
                </div>
                <div class="canvas-tools">
                  <el-button circle size="small" @click="zoomOut">-</el-button>
                  <span>{{ Math.round(canvasZoom * 100) }}%</span>
                  <el-button circle size="small" @click="zoomIn">+</el-button>
                </div>
              </div>

              <transition name="node-panel-slide">
                <aside v-if="selectedNode" class="node-panel node-panel-floating">
                  <div class="node-panel-header">
                    <div class="node-panel-heading">
                      <span class="node-panel-icon">
                        <el-icon><component :is="nodeIcon(selectedNode)" /></el-icon>
                      </span>
                      <div>
                        <strong>节点配置</strong>
                        <el-tag effect="plain" size="small">{{ workflowNodeTypeLabel(selectedNode) }}</el-tag>
                      </div>
                    </div>
                    <el-button :icon="Close" circle text @click="selectedNode = undefined" />
                  </div>
                  <div class="node-panel-body">
                    <div class="node-drawer-basic">
                      <label class="node-name-label">节点名称</label>
                      <el-input v-model="selectedNode.nodeName" class="node-name-input" placeholder="请输入节点名称" @input="syncDesignerJson" />
                    </div>
                  <el-form label-position="top">
                    <div class="node-config-section">
                      <div class="node-config-title">节点参数</div>
                      <template v-if="selectedNode.nodeType === 'EXCLUSIVE_BRANCH'">
                        <el-form-item label="条件表达式">
                          <el-input
                            v-model="selectedNode.conditionExpression"
                            placeholder="${amount > 100000 && tenantId == 1}"
                            @input="syncDesignerJson"
                            @change="value => parseConditionToBuilder(String(value || ''))"
                          />
                        </el-form-item>
                        <div class="condition-config-title">
                          <span>条件配置</span>
                          <el-button link type="primary" @click="addConditionRow">添加条件</el-button>
                        </div>
                        <div class="condition-builder">
                          <div v-for="(row, index) in conditionRows" :key="row.id" class="condition-row">
                            <el-select
                              v-if="index > 0"
                              v-model="row.connector"
                              class="condition-connector"
                              placeholder="关系"
                              @change="applyConditionBuilder"
                            >
                              <el-option label="并且 AND" value="AND" />
                              <el-option label="或者 OR" value="OR" />
                            </el-select>
                            <span v-else class="condition-connector-placeholder">当</span>
                            <el-select
                              v-model="row.variable"
                              class="condition-variable"
                              clearable
                              filterable
                              placeholder="选择变量"
                              @change="applyConditionBuilder"
                            >
                              <el-option-group
                                v-for="group in workflowVariableGroups"
                                :key="group.label"
                                :label="group.label"
                              >
                                <el-option
                                  v-for="item in group.options"
                                  :key="item.value"
                                  :label="`${item.label}（${item.value}）`"
                                  :value="item.value"
                                />
                              </el-option-group>
                            </el-select>
                            <el-select v-model="row.operator" class="condition-operator" placeholder="运算符" @change="applyConditionBuilder">
                              <el-option label="等于 ==" value="==" />
                              <el-option label="不等于 !=" value="!=" />
                              <el-option label="大于 >" value=">" />
                              <el-option label="大于等于 >=" value=">=" />
                              <el-option label="小于 <" value="<" />
                              <el-option label="小于等于 <=" value="<=" />
                            </el-select>
                            <el-input v-model="row.value" class="condition-value" placeholder="比较值" @input="applyConditionBuilder" />
                            <el-button link type="danger" :disabled="conditionRows.length === 1" @click="removeConditionRow(index)">删除</el-button>
                          </div>
                        </div>
                      </template>
                      <template v-else-if="isUserTaskNode(selectedNode)">
                        <el-form-item label="办理人类型">
                          <el-select :model-value="nodePropertyValue(selectedNode, 'assigneeType', 'USER')" @change="value => updateNodeProperty(selectedNode!, 'assigneeType', value)">
                            <el-option label="指定用户" value="USER" />
                            <el-option label="申请人" value="APPLICANT" />
                            <el-option label="申请人上级" value="APPLICANT_LEADER" />
                            <el-option label="角色" value="ROLE" />
                            <el-option label="机构岗位" value="ORG_POST" />
                            <el-option label="表达式" value="EXPRESSION" />
                          </el-select>
                        </el-form-item>
                        <el-form-item label="办理人表达式">
                          <el-input
                            :model-value="nodePropertyValue(selectedNode, 'assignee', '${initiator}')"
                            placeholder="${initiator}"
                            @input="value => updateNodeProperty(selectedNode!, 'assignee', value)"
                          />
                        </el-form-item>
                      </template>
                      <template v-else-if="isServiceTaskNode(selectedNode)">
                        <el-form-item v-if="selectedNode.executionType === 'SPRING_BEAN'" label="Bean 名称">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'beanName')" placeholder="workflowAuditService" @input="value => updateNodeProperty(selectedNode!, 'beanName', value)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'SPRING_BEAN'" label="方法名称">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'methodName')" placeholder="execute" @input="value => updateNodeProperty(selectedNode!, 'methodName', value)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'HTTP_URL'" label="请求地址">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'url')" placeholder="https://example.com/callback" @input="value => updateNodeProperty(selectedNode!, 'url', value)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'HTTP_URL'" label="请求方法">
                          <el-select :model-value="nodePropertyValue(selectedNode, 'method', 'POST')" @change="value => updateNodeProperty(selectedNode!, 'method', value)">
                            <el-option label="POST" value="POST" />
                            <el-option label="GET" value="GET" />
                            <el-option label="PUT" value="PUT" />
                            <el-option label="DELETE" value="DELETE" />
                          </el-select>
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'HTTP_URL'" label="超时时间">
                          <el-input-number :model-value="Number(nodePropertyValue(selectedNode, 'timeoutMillis', 5000))" :min="1000" :step="1000" @change="value => updateNodeProperty(selectedNode!, 'timeoutMillis', value || 5000)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'REMOTE_SERVICE'" label="服务名称">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'serviceName')" placeholder="guarantee-service" @input="value => updateNodeProperty(selectedNode!, 'serviceName', value)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'REMOTE_SERVICE'" label="操作编码">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'operation')" placeholder="submitBankMaterials" @input="value => updateNodeProperty(selectedNode!, 'operation', value)" />
                        </el-form-item>
                        <el-form-item v-if="selectedNode.executionType === 'EVENT_PUBLISH'" label="事件名称">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'eventName')" placeholder="workflow.cc" @input="value => updateNodeProperty(selectedNode!, 'eventName', value)" />
                        </el-form-item>
                        <el-form-item v-if="nodePropertyValue(selectedNode, 'businessStage')" label="业务阶段">
                          <el-input :model-value="nodePropertyValue(selectedNode, 'businessStage')" disabled />
                        </el-form-item>
                      </template>
                      <el-form-item v-if="hasAdvancedNodeProperties(selectedNode)" label="扩展属性 JSON">
                        <el-input
                          :model-value="formatNodeProperties(selectedNode)"
                          :rows="5"
                          type="textarea"
                          @change="value => updateNodeProperties(selectedNode!, value)"
                        />
                      </el-form-item>
                      <el-empty v-if="!hasNodeSpecificConfig(selectedNode)" description="当前节点暂无专属参数" :image-size="72" />
                    </div>
                    <el-form-item label="节点说明">
                      <el-input v-model="selectedNode.description" :rows="3" type="textarea" @input="syncDesignerJson" />
                    </el-form-item>
                  </el-form>
                  </div>
                </aside>
              </transition>
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
import { computed, defineComponent, h, nextTick, onMounted, reactive, ref, type PropType } from 'vue';
import { ElButton, ElIcon, ElMessage, ElMessageBox, ElPopover, type FormInstance, type FormRules } from 'element-plus';
import { Bell, Box, Cloudy, Close, Connection, ForkSpoon, PictureFilled, Plus, QuestionFilled, Refresh, Search, Setting, Share, User } from '@element-plus/icons-vue';
import FcDesigner, { type Config as FcDesignerConfig } from 'form-create-designer';
import type { Rule as FcRule } from '@form-create/element-ui';
import 'form-create-designer/src/style/index.css';
import 'form-create-designer/src/style/icon.css';
import { ImageUpload, UserSelector } from '@mango/common';
import {
  createNodeId,
  defaultDesignerJson,
  parseDesignerJson,
  stringifyDesignerJson,
  workflowApi,
  workflowStatusLabel,
  workflowStatusOptions,
  workflowStatusType,
  type WorkflowDefinition,
  type WorkflowDefinitionVersion,
  type WorkflowDesignerNode,
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
}

interface CustomFormConfig {
  submitPath: string;
  viewPath: string;
}

interface WorkflowVariableOption {
  label: string;
  value: string;
}

interface WorkflowVariableGroup {
  label: string;
  options: WorkflowVariableOption[];
}

interface ConditionRow {
  id: string;
  connector: 'AND' | 'OR';
  variable: string;
  operator: string;
  value: string;
}

interface ConditionGroup {
  id: string;
  connector: 'AND' | 'OR';
  rows: ConditionRow[];
}

const WorkflowNode = defineComponent({
  name: 'WorkflowNode',
  props: {
    node: { type: Object as PropType<WorkflowDesignerNode>, required: true },
    catalog: { type: Array as PropType<WorkflowNodeCatalog[]>, required: true },
    root: { type: Boolean, default: false },
    removable: { type: Boolean, default: false },
  },
  emits: ['select', 'changed', 'remove-self'],
  setup(props, { emit }) {
    const addPopoverVisible = ref(false);
    const addNode = (item: WorkflowNodeCatalog) => {
      const next: WorkflowDesignerNode = {
        id: createNodeId(item.nodeType.toLowerCase()),
        nodeDefinitionCode: item.nodeDefinitionCode,
        nodeName: item.nodeName,
        nodeType: item.nodeType,
        bpmnType: item.bpmnType,
        executionType: item.executionType,
        description: item.description,
        childNode: props.node.childNode || null,
        conditionNodes: [],
        properties: parseDefaultProperties(item.defaultProperties),
      };
      if (item.nodeType === 'EXCLUSIVE_GATEWAY' || item.nodeType === 'PARALLEL_GATEWAY') {
        next.conditionNodes = [
          branchNode('分支1', item.nodeType === 'EXCLUSIVE_GATEWAY' ? '${true}' : ''),
          branchNode('分支2', ''),
        ];
      }
      props.node.childNode = next;
      addPopoverVisible.value = false;
      emit('select', next);
      emit('changed');
    };

    const removeSelf = (event?: Event) => {
      event?.stopPropagation();
      emit('remove-self');
    };

    const removeCurrentChild = () => {
      props.node.childNode = props.node.childNode?.childNode || null;
      emit('changed');
    };

    const addBranch = () => {
      props.node.conditionNodes ||= [];
      props.node.conditionNodes.push(branchNode(`分支${props.node.conditionNodes.length + 1}`, ''));
      emit('changed');
    };

    const removeBranch = (index: number) => {
      props.node.conditionNodes?.splice(index, 1);
      emit('changed');
    };

    const renderAdd = () => h(ElPopover, {
      visible: addPopoverVisible.value,
      'onUpdate:visible': (value: boolean) => { addPopoverVisible.value = value; },
      trigger: 'click',
      width: 560,
      placement: 'right',
      popperClass: 'workflow-node-picker-popper',
    }, {
      reference: () => h(ElButton, { class: 'add-node-button', icon: Plus, circle: true, type: 'primary' }),
    default: () => h('div', { class: 'node-picker' }, groupedCatalog(props.catalog).map(group =>
        h('section', { class: 'node-picker-group' }, [
          h('div', { class: 'node-picker-title' }, group.name),
          h('div', { class: 'node-picker-grid' }, group.items.map(item =>
            h('button', { class: 'node-picker-item', type: 'button', onClick: () => addNode(item) }, [
              h('span', { class: 'node-picker-icon', style: { '--node-color': item.color || '#2563eb' } }, [
                h(ElIcon, null, () => h(nodeIcon(item))),
              ]),
              h('span', { class: 'node-picker-name' }, item.nodeName),
              h('span', { class: 'node-picker-desc' }, nodePickerDescription(item)),
            ]),
          )),
        ]),
      ).concat(groupedCatalog(props.catalog).length === 0
        ? [h('div', { class: 'node-picker-empty' }, '暂无可添加节点，请检查系统内置节点目录。')]
        : [])),
    });

    const renderCard = (node: WorkflowDesignerNode) => {
      const catalogItem = props.catalog.find(item => item.nodeDefinitionCode === node.nodeDefinitionCode || item.nodeType === node.nodeType);
      const icon = nodeIcon(catalogItem || node);
      const color = catalogItem?.color || (props.root ? '#64748b' : '#2563eb');
      return h('div', { class: ['workflow-node-card', props.root ? 'root' : '', node.nodeType === 'EXCLUSIVE_BRANCH' ? 'branch-node' : ''], style: { '--node-color': color }, onClick: () => emit('select', node) }, [
        h('div', { class: 'node-card-title' }, [
          h(ElIcon, null, () => h(icon)),
          h('span', { class: 'node-title-display', title: node.nodeName || '节点名称' }, node.nodeName || '节点名称'),
          props.removable ? h(ElButton, {
            class: 'node-card-delete',
            circle: true,
            text: true,
            icon: Close,
            title: '删除节点',
            onClick: removeSelf,
          }) : null,
        ]),
        h('div', { class: 'node-card-type' }, workflowNodeCardSummary(node)),
      ]);
    };

    return () => h('div', { class: 'workflow-node-wrap' }, [
      renderCard(props.node),
      props.node.nodeType === 'EXCLUSIVE_GATEWAY' || props.node.nodeType === 'PARALLEL_GATEWAY'
        ? h('div', { class: 'branch-area' }, [
            h(ElButton, { class: 'branch-add-button', link: true, type: 'primary', onClick: addBranch }, () => '添加分支'),
            h('div', { class: 'branch-list' }, (props.node.conditionNodes || []).map((branch, index) =>
              h('div', { class: 'branch-column' }, [
                h(WorkflowNode, {
                  node: branch,
                  catalog: props.catalog,
                  removable: true,
                  onSelect: (node: WorkflowDesignerNode) => emit('select', node),
                  onChanged: () => emit('changed'),
                  onRemoveSelf: () => removeBranch(index),
                }),
              ]),
            )),
            h('div', { class: 'branch-join-line' }),
          ])
        : null,
      renderAdd(),
      props.node.childNode ? h(WorkflowNode, {
        node: props.node.childNode,
        catalog: props.catalog,
        removable: true,
        onSelect: (node: WorkflowDesignerNode) => emit('select', node),
        onChanged: () => emit('changed'),
        onRemoveSelf: removeCurrentChild,
      }) : null,
    ]);
  },
});

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

const workflowIconOptions = [
  { value: 'User', label: '人员流程', component: User },
  { value: 'Bell', label: '通知提醒', component: Bell },
  { value: 'ForkSpoon', label: '条件审批', component: ForkSpoon },
  { value: 'Share', label: '并行协作', component: Share },
  { value: 'Box', label: '服务处理', component: Box },
  { value: 'Cloudy', label: '接口流程', component: Cloudy },
  { value: 'Connection', label: '外部连接', component: Connection },
];

const COMMON_DESIGNER_NODE_TYPES = new Set([
  'APPROVAL',
  'CC',
  'EXCLUSIVE_GATEWAY',
  'PARALLEL_GATEWAY',
  'SERVICE_BEAN',
  'SERVICE_HTTP',
  'SERVICE_REMOTE',
  'EVENT_PUBLISH',
]);

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
    rule: ruleFactory,
    props: () => [],
  };
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
    },
    options: [],
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
      data: [],
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
    },
    options: [],
  })),
  createWorkflowBusinessComponent('workflowRole', '角色', 'icon-group', () => ({
    type: 'select',
    field: createWorkflowBusinessField('roleId'),
    title: '角色',
    props: {
      placeholder: '请选择角色',
      clearable: true,
      filterable: true,
    },
    options: [],
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
const definitionStep = ref(0);
const canvasZoom = ref(1);
const conditionRows = ref<ConditionRow[]>([createConditionRow()]);
const workflowFormMode = ref<WorkflowFormMode>('DYNAMIC');
const workflowFormRules = ref<FcRule[]>(defaultWorkflowFormRules());
const customFormFields = ref<CustomFormField[]>([]);
const customFormConfig = reactive<CustomFormConfig>({ submitPath: '', viewPath: '' });
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

const workflowSystemVariableOptions: WorkflowVariableOption[] = [
  { label: '当前机构ID', value: 'tenantId' },
  { label: '当前机构编码', value: 'tenantCode' },
  { label: '申请人ID', value: 'applicant.id' },
  { label: '申请人姓名', value: 'applicant.name' },
  { label: '申请人手机号', value: 'applicant.mobile' },
  { label: '申请人组织ID', value: 'applicant.orgId' },
  { label: '申请人组织名称', value: 'applicant.orgName' },
  { label: '申请人岗位ID', value: 'applicant.postId' },
  { label: '申请人岗位名称', value: 'applicant.postName' },
  { label: '流程发起人', value: 'initiator' },
  { label: '流程启动时间', value: 'startTime' },
];

const workflowFormVariableOptions = computed(() => collectWorkflowFormVariables(workflowFormRules.value));

const workflowVariableGroups = computed<WorkflowVariableGroup[]>(() => [
  { label: '表单字段', options: workflowFormVariableOptions.value },
  { label: '系统内置参数', options: workflowSystemVariableOptions },
].filter(group => group.options.length > 0));

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
  designer.setMenuItem?.('business', []);
  designer.addComponent?.(workflowBusinessFormComponents);
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
  selectedNode.value = designerRoot.value;
  definitionStep.value = 0;
  canvasZoom.value = 1;
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

function zoomIn() {
  canvasZoom.value = Math.min(1.4, Number((canvasZoom.value + 0.1).toFixed(1)));
}

function zoomOut() {
  canvasZoom.value = Math.max(0.7, Number((canvasZoom.value - 0.1).toFixed(1)));
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

function branchNode(name: string, expression: string): WorkflowDesignerNode {
  return {
    id: createNodeId('branch'),
    nodeName: name,
    nodeType: 'EXCLUSIVE_BRANCH',
    conditionExpression: expression,
    childNode: null,
    conditionNodes: [],
    properties: {},
  };
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

function nodePickerDescription(item: WorkflowNodeCatalog) {
  if (item.description) {
    return item.description;
  }
  if (item.executionType === 'USER_TASK') {
    return '人工处理节点';
  }
  if (item.bpmnType?.includes('Gateway')) {
    return '控制流程分支和并行';
  }
  if (item.executionType && item.executionType !== 'NONE') {
    return `执行动作：${item.executionType}`;
  }
  return '通用流程节点';
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
      return {
        key,
        label,
        type: normalizeCustomFieldType(field?.type),
        required: Boolean(field?.required || field?.validate?.some?.((rule: any) => rule?.required) || field?.rules?.some?.((rule: any) => rule?.required)),
        optionsText: field?.optionsText || optionsToText(field?.options),
      };
    });
}

function normalizeCustomFieldType(type?: string) {
  if (type === 'number') return 'inputNumber';
  if (type === 'datetime') return 'datePicker';
  if (['input', 'textarea', 'inputNumber', 'select', 'datePicker'].includes(String(type))) {
    return String(type);
  }
  return 'input';
}

function customFieldsToFormCreateRules(fields: CustomFormField[]): FcRule[] {
  const rules = normalizeCustomFormFieldsValue(fields)
    .map((field): FcRule => {
      const rule: FcRule = {
        type: field.type,
        field: field.key,
        title: field.label,
        props: {
          placeholder: field.type === 'select' ? `请选择${field.label}` : `请输入${field.label}`,
        },
        validate: field.required
          ? [{ required: true, message: `${field.label}不能为空`, trigger: field.type === 'input' || field.type === 'textarea' ? 'blur' : 'change' }]
          : [],
      };
      if (field.type === 'select') {
        rule.options = textToOptions(field.optionsText);
      }
      return rule;
    });
  return rules.length > 0 ? rules : defaultWorkflowFormRules();
}

function formCreateRulesToCustomFields(rules: FcRule[]): CustomFormField[] {
  return collectWorkflowFormVariables(rules).map(variable => {
    const rule = findRuleByField(rules, variable.value) || {};
    return {
      key: variable.value,
      label: variable.label,
      type: normalizeCustomFieldType(String(rule.type || 'input')),
      required: Array.isArray(rule.validate) && rule.validate.some((item: any) => item?.required),
      optionsText: optionsToText(rule.options),
    };
  });
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
        variables.push({ label: item.title || item.field, value: item.field });
      }
      if (Array.isArray(item.children)) {
        visit(item.children);
      }
    }
  };
  visit(rules);
  return variables;
}

function isFormInputRule(rule: any) {
  return Boolean(rule?.field && rule.type !== 'hidden') || ['input', 'textarea', 'inputNumber', 'select', 'radio', 'checkbox', 'switch', 'datePicker', 'timePicker', 'cascader', 'treeSelect'].includes(String(rule?.type));
}

function parseDefaultProperties(value?: string) {
  if (!value) return {};
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function formatNodeProperties(node: WorkflowDesignerNode) {
  return JSON.stringify(node.properties || {}, null, 2);
}

function updateNodeProperties(node: WorkflowDesignerNode, value: string) {
  try {
    node.properties = JSON.parse(value || '{}');
    syncDesignerJson();
  } catch {
    ElMessage.error('节点属性必须是合法 JSON');
  }
}

function nodePropertyValue(node: WorkflowDesignerNode, key: string, fallback: any = '') {
  return node.properties?.[key] ?? fallback;
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

function isUserTaskNode(node: WorkflowDesignerNode) {
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
  return node.nodeType === 'EXCLUSIVE_BRANCH' || isUserTaskNode(node) || isServiceTaskNode(node) || hasAdvancedNodeProperties(node);
}

function createConditionRow(partial: Partial<ConditionRow> = {}): ConditionRow {
  return {
    id: createNodeId('condition'),
    connector: partial.connector || 'AND',
    variable: partial.variable || '',
    operator: partial.operator || '==',
    value: partial.value || '',
  };
}

function addConditionRow() {
  conditionRows.value.push(createConditionRow());
  applyConditionBuilder();
}

function removeConditionRow(index: number) {
  if (conditionRows.value.length <= 1) {
    return;
  }
  conditionRows.value.splice(index, 1);
  applyConditionBuilder();
}

function applyConditionBuilder() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'EXCLUSIVE_BRANCH') {
    return;
  }
  const parts = conditionRows.value
    .filter(row => row.variable)
    .map((row, index) => {
      const expression = `${row.variable} ${row.operator} ${formatConditionValue(row.value)}`;
      if (index === 0) {
        return expression;
      }
      return `${row.connector === 'OR' ? '||' : '&&'} ${expression}`;
    });
  if (parts.length === 0) {
    selectedNode.value.conditionExpression = '';
    syncDesignerJson();
    return;
  }
  selectedNode.value.conditionExpression = `\${${parts.join(' ')}}`;
  syncDesignerJson();
}

function parseConditionToBuilder(expression: string) {
  const inner = expression.match(/^\$\{\s*(.+?)\s*}$/)?.[1] || '';
  if (!inner) {
    conditionRows.value = [createConditionRow()];
    return;
  }
  const rows: ConditionRow[] = [];
  const regex = /(?:^|\s*(&&|\|\|)\s*)([A-Za-z_][\w.]*)\s*(==|!=|>=|<=|>|<)\s*('(?:\\'|[^'])*'|true|false|null|-?\d+(?:\.\d+)?|[^\s&|]+)/g;
  let matched: RegExpExecArray | null;
  while ((matched = regex.exec(inner)) !== null) {
    rows.push(createConditionRow({
      connector: matched[1] === '||' ? 'OR' : 'AND',
      variable: matched[2],
      operator: matched[3],
      value: unformatConditionValue(matched[4]),
    }));
  }
  conditionRows.value = rows.length > 0 ? rows : [createConditionRow()];
}

function formatConditionValue(value: string) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) return "''";
  if (/^(true|false|null)$/i.test(trimmed) || /^-?\d+(\.\d+)?$/.test(trimmed)) {
    return trimmed;
  }
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed;
  }
  return `'${trimmed.replace(/'/g, "\\'")}'`;
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
  const branches = node.conditionNodes || [];
  for (let index = 0; index < branches.length; index += 1) {
    const branch = branches[index];
    const shouldRequireCondition = node.nodeType === 'EXCLUSIVE_GATEWAY' && index < branches.length - 1;
    collectNodeErrors(branch, errors, shouldRequireCondition);
  }
  collectNodeErrors(node.childNode, errors);
}

function groupedCatalog(catalog: WorkflowNodeCatalog[]) {
  const groupsMap = new Map<string, WorkflowNodeCatalog[]>();
  for (const item of catalog) {
    if (item.nodeType === 'ROOT') continue;
    if (!COMMON_DESIGNER_NODE_TYPES.has(item.nodeType) && !COMMON_DESIGNER_NODE_TYPES.has(item.nodeDefinitionCode)) continue;
    const list = groupsMap.get(item.groupName) || [];
    list.push(item);
    groupsMap.set(item.groupName, list);
  }
  return Array.from(groupsMap.entries()).map(([name, items]) => ({ name, items }));
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
  display: inline-flex;
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
.designer-workbench,
.node-panel {
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
}

.workflow-icon-uploader.empty:hover .workflow-icon-empty-state {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.workflow-icon-empty-state .el-icon,
.workflow-icon-legacy-preview .el-icon {
  font-size: 20px;
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

.form-config-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.form-code-form {
  flex: 1;
  max-width: 420px;
}

.form-code-form :deep(.el-form-item) {
  margin-bottom: 0;
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

.workflow-form-designer {
  min-height: 560px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.custom-form-builder {
  display: grid;
  gap: 16px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: var(--el-bg-color);
}

.custom-route-section {
  display: grid;
  gap: 12px;
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

.designer-workbench {
  min-width: 0;
  overflow: hidden;
}

.designer-body {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 440px);
  gap: 14px;
  padding: 0;
  background: transparent;
  min-height: calc(100vh - 260px);
}

.node-canvas {
  position: relative;
  overflow: auto;
  padding: 14px 10px 36px;
  text-align: center;
}

.node-canvas-scale {
  display: inline-block;
  min-width: 620px;
  --workflow-line-color: #a8b3c7;
  --workflow-line-width: 2px;
  transform-origin: top center;
  transition: transform 0.16s ease-out;
}

.canvas-tools {
  position: sticky;
  right: 18px;
  bottom: 18px;
  z-index: 3;
  float: right;
  display: inline-flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.12);
  color: var(--el-text-color-secondary);
  font-weight: 700;
}

.node-panel {
  padding: 14px;
  align-self: start;
}

.panel-title {
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.node-config-section {
  display: grid;
  gap: 2px;
  margin: 10px 0 18px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.node-config-title {
  margin-bottom: 6px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

:deep(.workflow-node-wrap) {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

:deep(.workflow-node-card) {
  position: relative;
  z-index: 2;
  width: 220px;
  min-height: 72px;
  border: 1px solid color-mix(in srgb, var(--node-color) 36%, var(--el-border-color));
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: 0 4px 14px rgba(31, 41, 55, 0.08);
  cursor: pointer;
  overflow: hidden;
}

:deep(.workflow-node-card.branch-node) {
  border-style: dashed;
}

:deep(.workflow-node-card.root) {
  border-color: var(--el-color-primary-light-5);
}

:deep(.node-card-title) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  font-weight: 600;
  color: #fff;
  background: var(--node-color);
}

:deep(.node-title-display) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.node-card-delete) {
  margin-left: auto;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  color: rgba(255, 255, 255, 0.88);
}

:deep(.node-card-delete:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

:deep(.node-card-type) {
  padding: 10px 12px;
  text-align: left;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:deep(.add-node-button) {
  margin: 18px 0;
  position: relative;
  z-index: 3;
}

:deep(.add-node-button::before),
:deep(.add-node-button::after) {
  content: '';
  position: absolute;
  left: 50%;
  width: var(--workflow-line-width);
  height: 18px;
  background: var(--workflow-line-color);
  transform: translateX(-50%);
}

:deep(.add-node-button::before) {
  bottom: 100%;
}

:deep(.add-node-button::after) {
  top: 100%;
}

:deep(.branch-area) {
  position: relative;
  margin-top: 18px;
  min-width: 520px;
  padding: 24px 16px 16px;
  border: 1px dashed #c8d2e3;
  border-radius: 10px;
  background: #f7f9fc;
}

:deep(.branch-area::before) {
  content: '';
  position: absolute;
  top: 24px;
  left: 50%;
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
  transform: translate(-50%, -100%);
}

:deep(.branch-list) {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 16px;
  margin-top: 10px;
}

:deep(.branch-list::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 115px;
  right: 115px;
  height: var(--workflow-line-width);
  background: var(--workflow-line-color);
}

:deep(.branch-column) {
  position: relative;
  min-width: 230px;
  padding: 22px 8px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
}

:deep(.branch-column::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  width: var(--workflow-line-width);
  height: 22px;
  background: var(--workflow-line-color);
  transform: translateX(-50%);
}

.end-node {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 72px;
  height: 30px;
  margin-top: 4px;
  border-radius: 999px;
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.condition-config-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 8px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 700;
}

.condition-builder {
  display: grid;
  gap: 8px;
  margin: -8px 0 18px;
}

.condition-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 42px;
  gap: 8px;
  align-items: center;
}

.condition-variable {
  grid-column: span 2;
}

.condition-operator {
  grid-column: 1 / 2;
}

.condition-value {
  grid-column: 2 / 3;
}

.condition-connector,
.condition-connector-placeholder {
  width: 88px;
}

.condition-connector-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

:deep(.node-picker) {
  display: grid;
  gap: 14px;
}

:deep(.node-picker-group) {
  display: grid;
  gap: 8px;
}

:deep(.node-picker-title) {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

:deep(.node-picker-grid) {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, 1fr));
  gap: 8px;
}

:deep(.node-picker-item) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 78px;
  padding: 10px 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  text-align: center;
  cursor: pointer;
}

:deep(.node-picker-item:hover) {
  border-color: color-mix(in srgb, var(--node-color, #2563eb) 42%, var(--el-border-color));
  background: var(--el-fill-color-lighter);
}

:deep(.node-picker-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--node-color, #2563eb) 14%, #fff);
  color: var(--node-color, #2563eb);
}

:deep(.node-picker-name) {
  min-width: 0;
  width: 100%;
  font-weight: 700;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.node-picker-desc) {
  display: none;
}

:deep(.node-picker-empty) {
  padding: 18px 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
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
