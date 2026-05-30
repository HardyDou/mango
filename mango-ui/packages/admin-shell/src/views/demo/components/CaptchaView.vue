<template>
  <DemoDocLayout
    class="captcha-demo"
    title="验证码组件"
    subtitle="统一使用 CaptchaSelector 组件。示例按登录、风险确认、换绑和找回密码等真实业务流程组织。"
    content-box
    :toc-items="tocItems"
  >
    <section id="arithmetic" class="doc-section">
      <h2>算术验证码</h2>
      <p>适合登录、表单提交等轻量校验场景；用户输入计算结果后组件触发 success，业务表单再携带 captchaKey 提交。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="mock-form-card arithmetic-card">
            <div class="mock-form-head">
              <h3>表单提交校验</h3>
              <span>填写业务内容和验证码，点击提交时统一校验验证码。</span>
            </div>
            <div class="arithmetic-layout">
              <div class="arithmetic-form-side">
                <el-form class="mock-form" :model="arithmeticForm" label-position="top">
                  <el-form-item label="申请标题">
                    <el-input v-model="arithmeticForm.title" placeholder="请输入申请标题" />
                  </el-form-item>
                  <el-form-item label="申请说明">
                    <el-input v-model="arithmeticForm.description" type="textarea" :rows="3" placeholder="请输入申请说明" />
                  </el-form-item>
                </el-form>
                <div class="mock-actions">
                  <el-button type="primary" :disabled="!arithmeticCaptchaInput.trim()" @click="submitArithmeticDemo">
                    提交表单
                  </el-button>
                  <span>{{ arithmeticCaptchaInput.trim() ? '点击提交时校验验证码' : '请输入右侧验证码' }}</span>
                </div>
              </div>
              <div class="arithmetic-captcha-side">
                <div class="captcha-panel-title">图形验证码</div>
                <CaptchaSelector
                  ref="arithmeticCaptchaRef"
                  :type="CaptchaType.ARITHMETIC"
                  @success="(key, code, type) => handleSuccess('arithmetic', key, code, type)"
                  @refresh="() => handleRefresh('arithmetic')"
                  @input-change="handleArithmeticInputChange"
                />
              </div>
            </div>
            <div v-if="submitResults.arithmetic" class="mock-result">{{ submitResults.arithmetic }}</div>
          </div>
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
      <p>后端从真实图片库中选择背景，并从目标位置裁剪小图。前端展示背景缺口、拖动真实裁片并提交 pointJson 校验。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="mock-form-card risk-card">
            <div class="mock-form-head">
              <h3>风险操作确认</h3>
              <span>高风险操作提交前，先通过图片滑块确认操作者行为。</span>
            </div>
            <div class="risk-summary">
              <div>
                <span>操作类型</span>
                <strong>重置租户管理员密码</strong>
              </div>
              <div>
                <span>影响范围</span>
                <strong>tenant-prod-01</strong>
              </div>
            </div>
            <el-input v-model="blockForm.reason" type="textarea" :rows="2" placeholder="请输入操作原因" />
            <div class="slider-mode-list">
              <div class="slider-mode-item">
                <div class="mode-title">
                  <strong>触发式</strong>
                  <span>默认只占一行验证条，点击后展开拼图，不打断表单布局。</span>
                </div>
                <CaptchaSelector
                  :type="CaptchaType.BLOCK_PUZZLE"
                  mode="trigger"
                  @success="(key, code, type) => handleSuccess('block', key, code, type)"
                  @refresh="() => handleRefresh('block')"
                />
              </div>
              <div class="slider-mode-item">
                <div class="mode-title">
                  <strong>嵌入式</strong>
                  <span>直接展示完整拼图区域，适合安全确认页或空间充足的表单。</span>
                </div>
                <CaptchaSelector
                  :type="CaptchaType.BLOCK_PUZZLE"
                  mode="embedded"
                  @success="(key, code, type) => handleSuccess('block', key, code, type)"
                  @refresh="() => handleRefresh('block')"
                />
              </div>
              <div class="slider-mode-item">
                <div class="mode-title">
                  <strong>弹出式</strong>
                  <span>提交表单时自动弹出安全验证弹窗，验证通过后继续提交。</span>
                </div>
                <CaptchaSelector
                  ref="blockPopupCaptchaRef"
                  :type="CaptchaType.BLOCK_PUZZLE"
                  mode="popup"
                  @success="(key, code, type) => handleSuccess('blockPopup', key, code, type)"
                  @refresh="() => handleRefresh('blockPopup')"
                />
                <div class="mock-actions slider-popup-actions">
                  <el-button type="danger" @click="submitBlockPopupDemo">
                    提交表单并验证
                  </el-button>
                  <span>{{ captchaResults.blockPopup ? '弹窗滑块校验已通过' : '提交时会弹出滑块验证码' }}</span>
                </div>
                <div v-if="submitResults.blockPopup" class="mock-result">{{ submitResults.blockPopup }}</div>
              </div>
            </div>
            <div class="mock-actions">
              <el-button type="danger" :disabled="!captchaResults.block" @click="submitDemo('block')">
                确认执行
              </el-button>
              <span>{{ captchaResults.block ? '滑块校验已通过' : '拖动拼图块完成验证' }}</span>
            </div>
            <div v-if="submitResults.block" class="mock-result">{{ submitResults.block }}</div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('block')">
          <el-icon><component :is="codeVisible.block ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.block ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.block" :code="blockCode" />
      </div>
    </section>

    <section id="click-word" class="doc-section">
      <h2>点选文字验证码</h2>
      <p>适合风控二次确认场景；用户按提示顺序点击图片文字，组件内部提交点击坐标并完成校验。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="mock-form-card risk-card">
            <div class="mock-form-head">
              <h3>敏感配置发布</h3>
              <span>发布生产配置前，通过点选文字确认是真人操作。</span>
            </div>
            <div class="risk-summary">
              <div>
                <span>配置项</span>
                <strong>auth.login.policy</strong>
              </div>
              <div>
                <span>发布环境</span>
                <strong>production</strong>
              </div>
            </div>
            <div class="captcha-check-area">
              <CaptchaSelector
                :type="CaptchaType.CLICK_WORD"
                @success="(key, code, type) => handleSuccess('clickWord', key, code, type)"
                @refresh="() => handleRefresh('clickWord')"
              />
            </div>
            <div class="mock-actions">
              <el-button type="warning" :disabled="!captchaResults.clickWord" @click="submitDemo('clickWord')">
                确认发布
              </el-button>
              <span>{{ captchaResults.clickWord ? '点选文字校验已通过' : '请按提示点击图片文字' }}</span>
            </div>
            <div v-if="submitResults.clickWord" class="mock-result">{{ submitResults.clickWord }}</div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('clickWord')">
          <el-icon><component :is="codeVisible.clickWord ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.clickWord ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.clickWord" :code="clickWordCode" />
      </div>
    </section>

    <section id="behavior" class="doc-section">
      <h2>无感行为验证</h2>
      <p>适合登录、注册和高频表单提交。用户点击验证条后，组件提交已静默采集的行为和设备特征，后端返回 score、riskLevel 和 suggestAction。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="mock-form-card behavior-card">
            <div class="mock-form-head">
              <h3>登录风控校验</h3>
              <span>先点击验证条完成无感评分，再提交登录表单。</span>
            </div>
            <el-form class="mock-form" :model="behaviorForm" label-position="top">
              <el-form-item label="登录账号">
                <el-input v-model="behaviorForm.account" placeholder="请输入登录账号" />
              </el-form-item>
              <el-form-item label="登录密码">
                <el-input v-model="behaviorForm.password" type="password" placeholder="请输入登录密码" show-password />
              </el-form-item>
            </el-form>
            <div class="captcha-check-area">
              <CaptchaSelector
                :type="CaptchaType.BEHAVIOR"
                @success="(key, code, type) => handleSuccess('behavior', key, code, type)"
                @refresh="() => handleRefresh('behavior')"
              />
            </div>
            <div class="mock-actions">
              <el-button type="primary" :disabled="!captchaResults.behavior" @click="submitDemo('behavior')">
                登录
              </el-button>
              <span>{{ captchaResults.behavior ? '验证成功，可以登录' : '请先点击完成验证' }}</span>
            </div>
            <div v-if="submitResults.behavior" class="mock-result">{{ submitResults.behavior }}</div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('behavior')">
          <el-icon><component :is="codeVisible.behavior ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.behavior ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.behavior" :code="behaviorCode" />
      </div>
    </section>

    <section id="sms" class="doc-section">
      <h2>短信验证码</h2>
      <p>适合手机号确认、登录二次校验等场景；发送接口返回 captchaKey，输入验证码后统一走 verify 接口。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="mock-form-card verify-card">
            <div class="mock-form-head">
              <h3>手机号换绑</h3>
              <span>先校验新手机号，再提交换绑申请。</span>
            </div>
            <el-form class="mock-form" :model="smsForm" label-position="top">
              <el-form-item label="当前手机号">
                <el-input v-model="smsForm.oldMobile" disabled />
              </el-form-item>
              <el-form-item label="换绑原因">
                <el-select v-model="smsForm.scene" placeholder="请选择原因">
                  <el-option label="更换工作手机号" value="work-mobile" />
                  <el-option label="原手机号停用" value="mobile-disabled" />
                </el-select>
              </el-form-item>
            </el-form>
            <div class="captcha-check-area">
              <CaptchaSelector
                :type="CaptchaType.SMS"
                @success="(key, code, type) => handleSuccess('sms', key, code, type)"
              />
            </div>
            <div class="mock-actions">
              <el-button type="primary" :disabled="!captchaResults.sms" @click="submitDemo('sms')">
                提交换绑
              </el-button>
              <span>{{ captchaResults.sms ? '短信验证码已通过' : '请先发送并校验短信验证码' }}</span>
            </div>
            <div v-if="submitResults.sms" class="mock-result">{{ submitResults.sms }}</div>
          </div>
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
        <div class="demo-source">
          <div class="mock-form-card verify-card">
            <div class="mock-form-head">
              <h3>找回密码</h3>
              <span>通过邮箱验证码确认身份后，继续进入重置密码流程。</span>
            </div>
            <el-form class="mock-form" :model="emailForm" label-position="top">
              <el-form-item label="登录账号">
                <el-input v-model="emailForm.account" placeholder="请输入登录账号" />
              </el-form-item>
            </el-form>
            <div class="captcha-check-area">
              <CaptchaSelector
                :type="CaptchaType.EMAIL"
                @success="(key, code, type) => handleSuccess('email', key, code, type)"
              />
            </div>
            <div class="mock-actions">
              <el-button type="primary" :disabled="!captchaResults.email" @click="submitDemo('email')">
                下一步
              </el-button>
              <span>{{ captchaResults.email ? '邮件验证码已通过' : '请先发送并校验邮件验证码' }}</span>
            </div>
            <div v-if="submitResults.email" class="mock-result">{{ submitResults.email }}</div>
          </div>
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
        <div class="demo-source">
          <div class="mock-form-card verify-card">
            <div class="mock-form-head">
              <h3>安全校验</h3>
              <span>业务方不固定验证码类型时，交给组件选择器完成。</span>
            </div>
            <el-alert title="当前操作需要完成一次安全校验" type="warning" show-icon :closable="false" />
            <div class="captcha-check-area">
              <CaptchaSelector
                @success="(key, code, type) => handleSuccess('selector', key, code, type)"
                @refresh="() => handleRefresh('selector')"
              />
            </div>
            <div class="mock-actions">
              <el-button type="primary" :disabled="!captchaResults.selector" @click="submitDemo('selector')">
                确认提交
              </el-button>
              <span>{{ captchaResults.selector ? '验证码已通过' : '请选择并完成一种验证码' }}</span>
            </div>
            <div v-if="submitResults.selector" class="mock-result">{{ submitResults.selector }}</div>
          </div>
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
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { CaptchaSelector, CaptchaType } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

type DemoKey = 'arithmetic' | 'block' | 'blockPopup' | 'clickWord' | 'behavior' | 'sms' | 'email' | 'selector';

interface CaptchaResult {
  key: string;
  code?: string;
  type?: CaptchaType;
}

const tocItems = [
  { id: 'arithmetic', label: '算术验证码' },
  { id: 'block', label: '图片滑块验证码' },
  { id: 'click-word', label: '点选文字验证码' },
  { id: 'behavior', label: '无感行为验证' },
  { id: 'sms', label: '短信验证码' },
  { id: 'email', label: '邮件验证码' },
  { id: 'selector', label: '综合选择器' },
  { id: 'props', label: '支持属性' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'response', label: '返回字段' },
];

const codeVisible = ref<Record<DemoKey, boolean>>({
  arithmetic: false,
  block: false,
  blockPopup: false,
  clickWord: false,
  behavior: false,
  sms: false,
  email: false,
  selector: false,
});

const arithmeticCaptchaRef = ref<{ verify?: () => Promise<boolean> } | null>(null);
const blockPopupCaptchaRef = ref<{ verify?: () => Promise<boolean> } | null>(null);
const arithmeticCaptchaInput = ref('');
const arithmeticForm = reactive({ title: '组件库访问申请', description: '申请开通开发中心组件库的访问权限' });
const blockForm = reactive({ reason: '运维审批单 OPS-20260519 已通过' });
const behaviorForm = reactive({ account: 'admin', password: 'admin123' });
const smsForm = reactive({ oldMobile: '138****8000', scene: 'work-mobile' });
const emailForm = reactive({ account: 'admin@mango.local' });
const captchaResults = reactive<Partial<Record<DemoKey, CaptchaResult>>>({});
const submitResults = reactive<Partial<Record<DemoKey, string>>>({});

const arithmeticCode = `<template>
  <div class="submit-panel">
    <div class="form-side">
      <el-form :model="form" label-position="top">
        <el-form-item label="申请标题">
          <el-input v-model="form.title" />
        </el-form-item>
      </el-form>
      <el-button type="primary" :disabled="!captchaInput" @click="submit">
        提交表单
      </el-button>
    </div>
    <div class="captcha-side">
      <CaptchaSelector
        ref="captchaRef"
        :type="CaptchaType.ARITHMETIC"
        @success="handleCaptchaSuccess"
        @refresh="captchaResult = null"
        @input-change="captchaInput = $event"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
const captchaRef = ref<{ verify?: () => Promise<boolean> } | null>(null);
const captchaInput = ref('');

async function submit() {
  const verified = await captchaRef.value?.verify?.();
  if (!verified) return;
  // submit business form with captchaResult.key
}
<\\/script>`;

const blockCode = `<template>
  <div class="risk-confirm-panel">
    <div class="risk-summary">重置租户管理员密码</div>
    <el-input v-model="form.reason" type="textarea" />

    <!-- 触发式：点击验证条后展开拼图 -->
    <CaptchaSelector
      :type="CaptchaType.BLOCK_PUZZLE"
      mode="trigger"
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />

    <!-- 嵌入式：直接展示完整拼图区域 -->
    <CaptchaSelector
      :type="CaptchaType.BLOCK_PUZZLE"
      mode="embedded"
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />

    <!-- 弹出式：提交表单时自动打开安全验证弹窗 -->
    <CaptchaSelector
      ref="popupCaptchaRef"
      :type="CaptchaType.BLOCK_PUZZLE"
      mode="popup"
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />

    <el-button type="danger" @click="submit">
      确认执行
    </el-button>
  </div>
</template>

<script setup lang="ts">
const popupCaptchaRef = ref<{ verify?: () => Promise<boolean> } | null>(null);
const captchaResult = ref(null);

async function submit() {
  const passed = captchaResult.value || await popupCaptchaRef.value?.verify?.();
  if (!passed) return;
  // submit business form with captchaResult.key
}
<\\/script>`;

const clickWordCode = `<template>
  <div class="publish-confirm-panel">
    <div class="risk-summary">发布生产配置 auth.login.policy</div>

    <CaptchaSelector
      :type="CaptchaType.CLICK_WORD"
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />

    <el-button type="warning" :disabled="!captchaResult" @click="submit">
      确认发布
    </el-button>
  </div>
</template>`;

const behaviorCode = `<template>
  <div class="login-risk-panel">
    <el-form :model="form" label-position="top">
      <el-form-item label="登录账号">
        <el-input v-model="form.account" />
      </el-form-item>
      <el-form-item label="登录密码">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
    </el-form>

    <CaptchaSelector
      :type="CaptchaType.BEHAVIOR"
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />

    <el-button type="primary" :disabled="!captchaResult" @click="submit">
      登录
    </el-button>
  </div>
</template>

<script setup lang="ts">
// 用户点击验证条后组件会自动 verify；
// 登录按钮提交时携带 captchaResult.key 和 score。
<\\/script>`;

const smsCode = `<template>
  <div class="mobile-change-panel">
    <el-form :model="form" label-position="top">
      <el-form-item label="当前手机号">
        <el-input v-model="form.oldMobile" disabled />
      </el-form-item>
    </el-form>

    <CaptchaSelector :type="CaptchaType.SMS" @success="handleCaptchaSuccess" />

    <el-button type="primary" :disabled="!captchaResult" @click="submit">
      提交换绑
    </el-button>
  </div>
</template>`;

const emailCode = `<template>
  <div class="password-reset-panel">
    <el-input v-model="form.account" placeholder="请输入登录账号" />
    <CaptchaSelector :type="CaptchaType.EMAIL" @success="handleCaptchaSuccess" />
    <el-button type="primary" :disabled="!captchaResult" @click="submit">
      下一步
    </el-button>
  </div>
</template>`;

const selectorCode = `<template>
  <div class="security-check-panel">
    <CaptchaSelector
      @success="handleCaptchaSuccess"
      @refresh="captchaResult = null"
    />
    <el-button type="primary" :disabled="!captchaResult" @click="submit">
      确认提交
    </el-button>
  </div>
</template>`;

const propsTable = [
  { name: 'type', description: '固定验证码类型；不传时展示综合选择器', type: 'CaptchaType', defaultValue: '-' },
  { name: 'mode', description: '图片滑块 / Canvas 滑块展示形态；trigger 为触发式，embedded 为嵌入式，popup 为弹出式', type: "'trigger' | 'embedded' | 'popup'", defaultValue: 'embedded' },
];

const eventsTable = [
  { name: 'success', description: '验证码通过后触发，业务表单应保存 key 并在确认提交时携带', payload: '(key: string, code?: string, type?: CaptchaType) => void' },
  { name: 'refresh', description: '刷新图形验证码后触发，业务表单应清空已保存的验证码结果', payload: '() => void' },
  { name: 'refresh', description: '组件暴露方法，用于刷新当前验证码', payload: '() => void' },
];

const responseTable = [
  { name: 'key', description: '验证码键，提交业务接口或 verify 接口时使用', example: 'captcha-key' },
  { name: 'type', description: '验证码类型', example: 'ARITHMETIC / BLOCK_PUZZLE / CLICK_WORD / BEHAVIOR / SMS / EMAIL' },
  { name: 'image', description: '算术或点选文字验证码图片', example: 'data:image/png;base64,...' },
  { name: 'backgroundImage', description: '图片滑块背景图，已经绘制目标缺口', example: 'data:image/png;base64,...' },
  { name: 'sliderImage', description: '图片滑块小图，由后端从 backgroundImage 对应位置裁剪生成', example: 'data:image/png;base64,...' },
  { name: 'backgroundWidth', description: '图片滑块背景图生成宽度；前端按该宽度等比换算 x 坐标', example: '280' },
  { name: 'backgroundHeight', description: '图片滑块背景图生成高度；前端按该高度保持图片比例', example: '160' },
  { name: 'sliderSize', description: '图片滑块小图生成尺寸；前端按背景缩放比例同步缩放', example: '50' },
  { name: 'x', description: '图片滑块目标 X 坐标；组件内部提交 pointJson 时使用', example: '128' },
  { name: 'y', description: '图片滑块目标 Y 坐标；组件内部渲染小图高度时使用', example: '54' },
  { name: 'expireTime', description: '过期时间，单位秒', example: '300' },
  { name: 'target', description: '短信或邮件验证码发送目标；点选文字验证码中为点击顺序提示', example: '13800138000 / 云,山,月' },
  { name: 'extra', description: '点选文字验证码返回图片宽高和点击数量；正确坐标只保存在后端缓存', example: '{"width":320,"height":180,"pointCount":3}' },
  { name: 'score', description: '无感行为验证返回评分，0.0 到 1.0，分数越高越像真人', example: '0.86' },
  { name: 'riskLevel', description: '无感行为验证风险等级', example: 'LOW / MEDIUM / HIGH' },
  { name: 'suggestAction', description: '无感行为验证建议业务动作', example: 'ALLOW / SECONDARY_VERIFY / DENY' },
];

function handleSuccess(demo: DemoKey, key: string, code?: string, type?: CaptchaType) {
  captchaResults[demo] = { key, code, type };
  submitResults[demo] = '';
  ElMessage.success('验证码校验通过，可以确认提交');
}

function handleRefresh(demo: DemoKey) {
  captchaResults[demo] = undefined;
  submitResults[demo] = '';
  if (demo === 'arithmetic') {
    arithmeticCaptchaInput.value = '';
  }
}

function handleArithmeticInputChange(value: string) {
  arithmeticCaptchaInput.value = value;
  captchaResults.arithmetic = undefined;
  submitResults.arithmetic = '';
}

async function submitArithmeticDemo() {
  if (!arithmeticCaptchaInput.value.trim()) {
    ElMessage.warning('请输入验证码');
    return;
  }
  const verified = await arithmeticCaptchaRef.value?.verify?.();
  if (!verified) return;
  submitDemo('arithmetic');
}

async function submitBlockPopupDemo() {
  if (!captchaResults.blockPopup) {
    const verified = await blockPopupCaptchaRef.value?.verify?.();
    if (!verified) return;
  }
  submitDemo('blockPopup');
}

function submitDemo(demo: DemoKey) {
  const result = captchaResults[demo];
  if (!result) {
    ElMessage.warning('请先完成验证码');
    return;
  }
  let detail = '';
  if (demo === 'behavior' && result.code) {
    try {
      const behaviorResult = JSON.parse(result.code) as { score?: number; riskLevel?: string; suggestAction?: string };
      detail = `，score=${behaviorResult.score}，riskLevel=${behaviorResult.riskLevel}，suggestAction=${behaviorResult.suggestAction}`;
    } catch {
      detail = '';
    }
  } else {
    detail = result.code ? `，code=${result.code}` : '';
  }
  submitResults[demo] = `已提交：type=${result.type ?? 'UNKNOWN'}，captchaKey=${result.key}${detail}`;
}

function toggleCode(key: DemoKey) {
  codeVisible.value[key] = !codeVisible.value[key];
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.mock-form-card {
  width: min(460px, 100%);
  padding: 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
  box-shadow: 0 8px 24px rgb(31 45 61 / 7%);
  text-align: left;
}

.captcha-demo {
  :deep(.page-header),
  :deep(.doc-section),
  :deep(.mock-form-head),
  :deep(.mode-title),
  :deep(.captcha-panel-title) {
    text-align: left;
  }

  :deep(.demo-source) {
    display: flex;
    justify-content: center;
  }
}

.arithmetic-card {
  width: min(760px, 100%);
}

.verify-card {
  max-width: 420px;
}

.risk-card {
  width: min(680px, 100%);
}

.behavior-card {
  width: min(480px, 100%);
}

.mock-form-head {
  margin-bottom: 18px;

  h3 {
    margin: 0 0 6px;
    font-size: 18px;
    font-weight: 500;
    line-height: 1.4;
  }

  span {
    color: var(--el-text-color-regular);
    font-size: 14px;
    line-height: 1.6;
  }
}

.mock-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-select) {
    width: 100%;
  }
}

.arithmetic-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 320px);
  gap: 24px;
  align-items: start;
}

.arithmetic-form-side {
  min-width: 0;
}

.arithmetic-captcha-side {
  min-width: 0;
  padding-left: 24px;
  border-left: 1px solid var(--el-border-color-lighter);

  :deep(.captcha-card) {
    width: 100%;
  }
}

.captcha-panel-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

.captcha-check-area {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
  align-items: center;

  :deep(.captcha-card),
  :deep(.captcha-form) {
    width: 100%;
    max-width: 420px;
    align-self: center;
    text-align: left;
  }

  :deep(.captcha-form .row) {
    flex-wrap: wrap;
  }

  :deep(.captcha-form .row .el-input) {
    flex: 1 1 180px;
  }
}

.slider-mode-list {
  display: grid;
  gap: 18px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.slider-mode-item {
  display: grid;
  gap: 10px;
  justify-items: center;

  :deep(.captcha-card) {
    width: 100%;
    max-width: 420px;
    text-align: left;
  }
}

.mode-title {
  display: grid;
  gap: 4px;
  justify-self: stretch;

  strong {
    color: var(--el-text-color-primary);
    font-size: 15px;
    font-weight: 500;
  }

  span {
    color: var(--el-text-color-regular);
    font-size: 14px;
    line-height: 1.6;
  }
}

.mock-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;

  .el-button {
    min-width: 104px;
  }

  span {
    color: var(--el-text-color-regular);
    font-size: 14px;
  }
}

.mock-result {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 4px;
  background: var(--el-color-success-light-9);
  color: var(--el-color-success-dark-2);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.risk-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;

  div {
    padding: 12px;
    border-radius: 4px;
    background: var(--el-fill-color-lighter);
  }

  span {
    display: block;
    margin-bottom: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 500;
  }
}

@media (max-width: 768px) {
  .mock-form-card {
    padding: 16px;
  }

  .risk-summary {
    grid-template-columns: 1fr;
  }

  .arithmetic-layout {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .arithmetic-captcha-side {
    padding-top: 18px;
    padding-left: 0;
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: 0;
  }

  .mock-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
