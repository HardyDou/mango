<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="profile-container" data-page="account.profile">
    <el-card class="profile-shell" shadow="never">
      <div class="settings-layout">
        <aside class="settings-sidebar" data-surface="profile.navigation">
          <div class="settings-sidebar__heading">
            <span>个人中心</span>
            <h2>账户设置</h2>
          </div>

          <component :is="profileSlots.sidebarTop" v-if="profileSlots.sidebarTop" :user="profile" />

          <div class="sidebar-user">
            <el-avatar :size="48" :src="profileAvatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div>
              <strong>{{ displayName }}</strong>
              <span>{{ profile?.username || roleLabel }}</span>
            </div>
          </div>

          <el-menu :default-active="activeSection" class="settings-menu" @select="handleSectionSelect">
            <el-menu-item index="profile" data-action="switch-profile">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="security" data-action="switch-security">
              <el-icon><Lock /></el-icon>
              <span>账号安全</span>
            </el-menu-item>
            <el-menu-item index="authorization" data-action="switch-authorization">
              <el-icon><Connection /></el-icon>
              <span>第三方授权</span>
            </el-menu-item>
            <el-menu-item index="password" data-action="switch-password">
              <el-icon><Key /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item v-if="profileSlots.theme" index="theme" data-action="switch-theme">
              <el-icon><Brush /></el-icon>
              <span>主题设置</span>
            </el-menu-item>
          </el-menu>

          <component :is="profileSlots.sidebarBottom" v-if="profileSlots.sidebarBottom" :user="profile" />
        </aside>

        <main v-loading="loading" class="settings-content" :data-state="activeSection">
          <header class="content-header">
            <div>
              <span class="content-eyebrow">账户设置</span>
              <h1>{{ sectionTitle }}</h1>
              <p>{{ sectionDescription }}</p>
            </div>
          </header>

          <Transition name="content-fade" mode="out-in">
            <div v-if="activeSection === 'profile'" key="profile" data-surface="profile.details">
              <component :is="profileSlots.infoBefore" v-if="profileSlots.infoBefore" :form="form" :user="profile" />

              <el-form label-position="top" class="profile-form">
                <section class="content-section" aria-labelledby="basic-profile-title">
                  <div class="section-heading">
                    <div>
                      <h2 id="basic-profile-title">基础信息</h2>
                      <p>头像和昵称会用于个人中心及账户标识。</p>
                    </div>
                  </div>

                  <el-form-item label="头像" data-field="avatar">
                    <div class="avatar-editor">
                      <div class="avatar-preview">
                        <el-avatar :size="88" :src="profileAvatar">
                          <el-icon><User /></el-icon>
                        </el-avatar>
                      </div>
                      <div class="avatar-actions">
                        <div class="avatar-action-row">
                          <el-upload
                            v-model:file-list="avatarFileList"
                            accept=".jpg,.jpeg,.png,.webp"
                            :auto-upload="false"
                            :show-file-list="false"
                            :disabled="saving"
                            :on-change="handleAvatarChange"
                          >
                            <el-button :disabled="saving">
                              <el-icon class="el-icon--left"><UploadIcon /></el-icon>
                              {{ hasAvatar ? '更换头像' : '选择图片' }}
                            </el-button>
                          </el-upload>
                          <el-button v-if="hasAvatar" link type="danger" :disabled="saving" @click="removeAvatar">
                            移除
                          </el-button>
                        </div>
                        <p>支持 JPG、PNG、WebP，图片不超过 5 MB；点击保存后生效。</p>
                      </div>
                    </div>
                  </el-form-item>

                  <el-row :gutter="20">
                    <el-col :xs="24" :md="12">
                      <el-form-item label="用户名">
                        <el-input :model-value="profile?.username || ''" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="12">
                      <el-form-item label="昵称">
                        <el-input v-model="form.nickname" maxlength="100" show-word-limit />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </section>

                <section class="content-section" aria-labelledby="identity-profile-title">
                  <div class="section-heading">
                    <div>
                      <h2 id="identity-profile-title">实名信息</h2>
                      <p>用于账户身份记录；录入后默认保持未认证，认证来源为空。</p>
                    </div>
                    <div class="verification-summary">
                      <el-tag :type="verificationTagType">{{ verificationStatusLabel }}</el-tag>
                      <span>来源：{{ profile?.verificationSource || '无' }}</span>
                    </div>
                  </div>

                  <el-row :gutter="20">
                    <el-col :xs="24" :md="12">
                      <el-form-item label="姓名" data-field="real-name">
                        <el-input v-model="form.realName" maxlength="100" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="12">
                      <el-form-item label="证件类型">
                        <el-select v-model="form.documentType" clearable placeholder="请选择" style="width: 100%">
                          <el-option label="居民身份证" value="ID_CARD" />
                          <el-option label="护照" value="PASSPORT" />
                          <el-option label="其他证件" value="OTHER" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="证件号码" data-field="document-number">
                    <el-input v-model="form.documentNumber" maxlength="128" :placeholder="documentNumberPlaceholder" />
                    <div class="field-hint">留空表示不修改；证件类型和号码都清空表示删除。</div>
                  </el-form-item>
                </section>

                <div class="form-actions">
                  <el-button type="primary" :loading="saving" data-action="save-profile" @click="saveProfile">
                    保存资料
                  </el-button>
                </div>
              </el-form>

              <component :is="profileSlots.infoAfter" v-if="profileSlots.infoAfter" :form="form" :user="profile" />
            </div>

            <div v-else-if="activeSection === 'security'" key="security" data-surface="profile.security">
              <section class="content-section content-section--first" aria-labelledby="account-security-title">
                <div class="section-heading">
                  <div>
                    <h2 id="account-security-title">联系方式</h2>
                    <p>修改手机号或邮箱时，需要验证当前密码和新联系方式验证码。</p>
                  </div>
                </div>

                <div class="security-list">
                  <div class="security-item" data-field="contact-phone">
                    <div class="security-icon">
                      <el-icon><Iphone /></el-icon>
                    </div>
                    <div class="security-copy">
                      <strong>手机号</strong>
                      <span>{{ profile?.phone || '未设置' }}</span>
                    </div>
                    <el-button type="primary" link @click="openContactDialog('PHONE')">修改手机号</el-button>
                  </div>

                  <div class="security-item" data-field="contact-email">
                    <div class="security-icon">
                      <el-icon><Message /></el-icon>
                    </div>
                    <div class="security-copy">
                      <strong>邮箱</strong>
                      <span>{{ profile?.email || '未设置' }}</span>
                    </div>
                    <el-button type="primary" link @click="openContactDialog('EMAIL')">修改邮箱</el-button>
                  </div>
                </div>
              </section>
            </div>

            <div
              v-else-if="activeSection === 'authorization'"
              key="authorization"
              data-surface="profile.authorizations"
            >
              <section class="content-section content-section--first" aria-labelledby="authorization-title">
                <div class="authorization-header">
                  <div>
                    <h2 id="authorization-title">第三方账号</h2>
                    <p>绑定后可使用对应账号登录当前应用；解绑时需要验证当前密码。</p>
                  </div>
                  <el-button :loading="authorizationLoading" @click="loadAuthorizations">刷新</el-button>
                </div>

                <el-table v-loading="authorizationLoading" :data="providerRows" empty-text="暂无可用登录方式">
                  <el-table-column label="登录方式" min-width="140">
                    <template #default="{ row }">
                      <span :data-provider="row.provider">{{ row.displayName }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="绑定账号" min-width="180">
                    <template #default="{ row }">
                      {{ row.binding?.displayName || row.binding?.externalUserId || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                      <el-tag v-if="row.binding" type="success" size="small">已绑定</el-tag>
                      <el-tag v-else-if="row.available" type="info" size="small">未绑定</el-tag>
                      <el-tag v-else type="warning" size="small">未配置</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" fixed="right">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.binding"
                        link
                        type="danger"
                        data-action="unbind"
                        @click="openUnbindDialog(row.binding)"
                      >
                        解绑
                      </el-button>
                      <el-button
                        v-else
                        link
                        type="primary"
                        data-action="bind"
                        :disabled="!row.available || bindingProvider === row.provider"
                        :loading="bindingProvider === row.provider"
                        @click="bindProvider(row.provider)"
                      >
                        绑定
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </section>
            </div>

            <div v-else-if="activeSection === 'password'" key="password" data-surface="profile.password">
              <PasswordView display-mode="embedded" />
            </div>

            <div v-else key="theme" data-surface="profile.theme">
              <component :is="profileSlots.theme" embedded />
            </div>
          </Transition>

          <div v-if="profileSlots.extraTabs" class="profile-extension">
            <component :is="profileSlots.extraTabs" :form="form" :user="profile" />
          </div>
        </main>
      </div>
    </el-card>

    <el-dialog v-model="contactDialogVisible" :title="contactDialogTitle" width="500px">
      <el-form label-width="104px">
        <el-form-item :label="contactType === 'PHONE' ? '新手机号' : '新邮箱'" required>
          <el-input v-model="contactForm.target" autocomplete="off" />
        </el-form-item>
        <el-form-item label="验证码" required>
          <div class="captcha-row">
            <el-input v-model="contactForm.captchaCode" maxlength="12" />
            <el-button :loading="captchaSending" :disabled="captchaCountdown > 0" @click="sendContactCaptcha">
              {{ captchaCountdown > 0 ? `${captchaCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="当前密码" required>
          <el-input
            v-model="contactForm.currentPassword"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactSaving" @click="saveContact">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="unbindDialogVisible" title="解绑第三方账号" width="460px">
      <p class="dialog-description">解绑后将不能再用该第三方账号登录当前应用。</p>
      <el-form label-width="96px">
        <el-form-item label="当前密码" required>
          <el-input v-model="unbindPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unbindDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="unbinding" @click="confirmUnbind">确认解绑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MangoAuthProfile">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type UploadProps, type UploadUserFile } from 'element-plus';
import { Brush, Connection, Iphone, Key, Lock, Message, Upload as UploadIcon, User } from '@element-plus/icons-vue';
import { downloadUploadedFile, fileToken, uploadImage } from '@mango/common/api/upload';
import { Session } from '@mango/common/utils/storage';
import { useAuthConfig } from '../composables/useAuthConfig';
import {
  getCurrentUserProfile,
  listCurrentExternalIdentities,
  sendCurrentContactCaptcha,
  unbindCurrentExternalIdentity,
  updateCurrentUserContact,
  updateCurrentUserProfile,
  type ContactType,
  type CurrentUserProfile,
  type ExternalAuthProvider,
  type ExternalIdentityBinding,
} from '../api/identity';
import {
  listAvailableProviders,
  providerCallbackUri,
  startProviderAuthorization,
  type AvailableProvider,
} from '../api/provider';
import PasswordView from './password.vue';

type ProfileSection = 'profile' | 'security' | 'authorization' | 'password' | 'theme';

const PROVIDER_RETURN_KEY = 'mango-auth:provider-return';
const AVATAR_MAX_SIZE = 5 * 1024 * 1024;
const AVATAR_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const authConfig = useAuthConfig();
const route = useRoute();
const router = useRouter();
const sessionUser = Session.get('userInfo') || {};
const tenantId = String(sessionUser.tenantId || Session.get('tenantId') || '');
const appCode = String(sessionUser.appCode || 'internal-admin');
const activeSection = ref<ProfileSection>(normalizeProfileSection(route.query.tab));
const loading = ref(false);
const saving = ref(false);
const avatarUploading = ref(false);
const authorizationLoading = ref(false);
const bindingProvider = ref<ExternalAuthProvider>();
const profile = ref<CurrentUserProfile>();
const availableProviders = ref<AvailableProvider[]>([]);
const bindings = ref<ExternalIdentityBinding[]>([]);
const form = reactive({ nickname: '', avatar: '', realName: '', documentType: '', documentNumber: '' });
const existingDocumentNumber = ref('');

const avatarFileList = ref<UploadUserFile[]>([]);
const pendingAvatarFile = ref<File>();
const pendingAvatarPreviewUrl = ref('');
const savedAvatarPreviewUrl = ref('');
const savedAvatarObjectUrl = ref('');
const avatarCleared = ref(false);
let avatarPreviewRequestId = 0;

const contactDialogVisible = ref(false);
const contactType = ref<ContactType>('PHONE');
const contactSaving = ref(false);
const captchaSending = ref(false);
const captchaCountdown = ref(0);
const contactForm = reactive({ target: '', currentPassword: '', captchaKey: '', captchaCode: '' });
let countdownTimer: number | undefined;

const unbindDialogVisible = ref(false);
const unbinding = ref(false);
const unbindPassword = ref('');
const selectedBinding = ref<ExternalIdentityBinding>();

const profileSlots = computed(() => authConfig.value.profile?.slots || {});
const displayName = computed(() => profile.value?.nickname || profile.value?.username || '用户');
const roleLabel = computed(() => authConfig.value.profile?.roleLabel || sessionUser.roleName || '当前用户');
const profileAvatar = computed(() => {
  if (pendingAvatarPreviewUrl.value) return pendingAvatarPreviewUrl.value;
  if (avatarCleared.value) return '';
  return (
    savedAvatarPreviewUrl.value ||
    displayableAvatarUrl(form.avatar) ||
    displayableAvatarUrl(authConfig.value.profile?.avatarUrl) ||
    displayableAvatarUrl(sessionUser.photo) ||
    ''
  );
});
const hasAvatar = computed(() => Boolean(profileAvatar.value || form.avatar || pendingAvatarFile.value));
const verificationStatusLabel = computed(() =>
  profile.value?.verificationStatus === 'VERIFIED' ? '已认证' : '未认证',
);
const verificationTagType = computed(() => (profile.value?.verificationStatus === 'VERIFIED' ? 'success' : 'info'));
const documentNumberPlaceholder = computed(() =>
  existingDocumentNumber.value ? `已设置（${existingDocumentNumber.value}），留空不修改` : '请输入证件号码',
);
const contactDialogTitle = computed(() => (contactType.value === 'PHONE' ? '修改手机号' : '修改邮箱'));
const sectionTitle = computed(() => {
  if (activeSection.value === 'security') return '账号安全';
  if (activeSection.value === 'authorization') return '第三方授权';
  if (activeSection.value === 'password') return '修改密码';
  if (activeSection.value === 'theme') return '主题设置';
  return '个人资料';
});
const sectionDescription = computed(() => {
  if (activeSection.value === 'security') return '管理用于登录验证和账号找回的联系方式。';
  if (activeSection.value === 'authorization') return '查看并管理当前应用的第三方登录授权。';
  if (activeSection.value === 'password') return '定期更新登录密码，保护当前账号安全。';
  if (activeSection.value === 'theme') return '调整布局、颜色和界面显示偏好，设置会在当前浏览器生效。';
  return '维护你的基础资料、头像和实名信息。';
});
const providerRows = computed(() =>
  (['WECOM', 'DINGTALK'] as ExternalAuthProvider[]).map((provider) => ({
    provider,
    displayName: provider === 'WECOM' ? '企业微信' : '钉钉',
    available: availableProviders.value.some((item) => item.provider === provider),
    binding: bindings.value.find((item) => item.provider === provider),
  })),
);

watch(
  () => route.query.tab,
  (value) => {
    activeSection.value = normalizeProfileSection(value);
  },
);

onMounted(() => {
  void Promise.all([loadProfile(), loadAuthorizations()]);
});

onBeforeUnmount(() => {
  if (countdownTimer != null) {
    window.clearInterval(countdownTimer);
  }
  clearPendingAvatarPreview();
  revokeSavedAvatarObjectUrl();
});

function normalizeProfileSection(value: unknown): ProfileSection {
  if (value === 'authorization') return 'authorization';
  if (value === 'security') return 'security';
  if (value === 'password') return 'password';
  if (value === 'theme' && authConfig.value.profile?.slots?.theme) return 'theme';
  return 'profile';
}

function handleSectionSelect(index: string) {
  const section = normalizeProfileSection(index);
  activeSection.value = section;
  const query = { ...route.query, tab: section === 'profile' ? undefined : section };
  void router.replace({ query });
}

async function loadProfile() {
  loading.value = true;
  try {
    const data = await getCurrentUserProfile();
    profile.value = data;
    form.nickname = data.nickname || '';
    form.avatar = data.avatar || '';
    form.realName = data.realName || '';
    form.documentType = data.documentType || '';
    form.documentNumber = '';
    existingDocumentNumber.value = data.documentNumber || '';
    avatarCleared.value = false;
    pendingAvatarFile.value = undefined;
    avatarFileList.value = [];
    clearPendingAvatarPreview();
    await resolveSavedAvatar(data.avatar);
  } catch (error) {
    console.error('加载个人资料失败:', error);
  } finally {
    loading.value = false;
  }
}

const handleAvatarChange: UploadProps['onChange'] = (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;
  if (!AVATAR_MIME_TYPES.has(file.type)) {
    avatarFileList.value = [];
    ElMessage.error('头像仅支持 JPG、PNG 或 WebP 图片');
    return;
  }
  if (file.size > AVATAR_MAX_SIZE) {
    avatarFileList.value = [];
    ElMessage.error('头像图片不能超过 5 MB');
    return;
  }
  clearPendingAvatarPreview();
  pendingAvatarFile.value = file;
  pendingAvatarPreviewUrl.value = URL.createObjectURL(file);
  avatarFileList.value = [uploadFile];
  avatarCleared.value = false;
  ElMessage.success('头像已选择，保存资料后生效');
};

function removeAvatar() {
  pendingAvatarFile.value = undefined;
  avatarFileList.value = [];
  clearPendingAvatarPreview();
  form.avatar = '';
  avatarCleared.value = true;
}

async function saveProfile() {
  if (saving.value) return;
  saving.value = true;
  try {
    let avatar = form.avatar.trim();
    if (pendingAvatarFile.value) {
      avatarUploading.value = true;
      const uploaded = await uploadImage(pendingAvatarFile.value);
      avatar = fileToken(uploaded.id);
      if (!avatar) {
        throw new Error('头像上传成功但未返回文件标识');
      }
      form.avatar = avatar;
      pendingAvatarFile.value = undefined;
      avatarFileList.value = [];
    }
    const data = await updateCurrentUserProfile({
      nickname: form.nickname.trim() || undefined,
      avatar: avatar || undefined,
      realName: form.realName.trim() || undefined,
      documentType: form.documentType || undefined,
      documentNumber: form.documentNumber.trim() || undefined,
    });
    profile.value = data;
    form.avatar = data.avatar || '';
    existingDocumentNumber.value = data.documentNumber || '';
    form.documentNumber = '';
    avatarCleared.value = false;
    await resolveSavedAvatar(data.avatar);
    clearPendingAvatarPreview();
    Session.set('userInfo', { ...sessionUser, nickname: data.nickname, photo: data.avatar });
    ElMessage.success('资料已保存');
  } catch (error) {
    console.error('保存个人资料失败:', error);
  } finally {
    avatarUploading.value = false;
    saving.value = false;
  }
}

async function resolveSavedAvatar(value?: string) {
  const requestId = ++avatarPreviewRequestId;
  revokeSavedAvatarObjectUrl();
  savedAvatarPreviewUrl.value = '';
  const directUrl = displayableAvatarUrl(value);
  if (directUrl) {
    savedAvatarPreviewUrl.value = directUrl;
    return;
  }
  const fileId = avatarFileId(value);
  if (!fileId) return;
  try {
    const response = await downloadUploadedFile(fileId);
    const blob =
      response.data instanceof Blob
        ? response.data
        : new Blob([response.data], { type: response.headers?.['content-type'] || 'application/octet-stream' });
    const objectUrl = URL.createObjectURL(blob);
    if (requestId !== avatarPreviewRequestId) {
      URL.revokeObjectURL(objectUrl);
      return;
    }
    savedAvatarObjectUrl.value = objectUrl;
    savedAvatarPreviewUrl.value = objectUrl;
  } catch (error) {
    console.error('加载头像失败:', error);
  }
}

function avatarFileId(value?: string) {
  const match = String(value || '')
    .trim()
    .match(/^mango-file:([1-9]\d*)$/u);
  return match?.[1] || '';
}

function displayableAvatarUrl(value?: string) {
  const normalized = String(value || '').trim();
  return /^(https?:|data:|blob:|\/)/iu.test(normalized) ? normalized : '';
}

function clearPendingAvatarPreview() {
  if (pendingAvatarPreviewUrl.value) {
    URL.revokeObjectURL(pendingAvatarPreviewUrl.value);
    pendingAvatarPreviewUrl.value = '';
  }
}

function revokeSavedAvatarObjectUrl() {
  if (savedAvatarObjectUrl.value) {
    URL.revokeObjectURL(savedAvatarObjectUrl.value);
    savedAvatarObjectUrl.value = '';
  }
}

function openContactDialog(type: ContactType) {
  contactType.value = type;
  Object.assign(contactForm, { target: '', currentPassword: '', captchaKey: '', captchaCode: '' });
  captchaCountdown.value = 0;
  contactDialogVisible.value = true;
}

async function sendContactCaptcha() {
  const target = contactForm.target.trim();
  if (!target) {
    ElMessage.warning(`请输入新${contactType.value === 'PHONE' ? '手机号' : '邮箱'}`);
    return;
  }
  captchaSending.value = true;
  try {
    const ticket = await sendCurrentContactCaptcha({ contactType: contactType.value, target });
    contactForm.captchaKey = ticket.key;
    ElMessage.success(`验证码已发送至 ${ticket.target}`);
    startCountdown(Math.min(ticket.expiresInSeconds || 60, 60));
  } catch (error) {
    console.error('发送联系方式验证码失败:', error);
  } finally {
    captchaSending.value = false;
  }
}

async function saveContact() {
  if (
    !contactForm.target.trim() ||
    !contactForm.currentPassword ||
    !contactForm.captchaKey ||
    !contactForm.captchaCode
  ) {
    ElMessage.warning('请填写新联系方式、验证码和当前密码');
    return;
  }
  contactSaving.value = true;
  try {
    profile.value = await updateCurrentUserContact({
      contactType: contactType.value,
      target: contactForm.target.trim(),
      currentPassword: contactForm.currentPassword,
      captchaKey: contactForm.captchaKey,
      captchaCode: contactForm.captchaCode.trim(),
    });
    contactDialogVisible.value = false;
    ElMessage.success('联系方式已更新');
  } catch (error) {
    console.error('修改联系方式失败:', error);
  } finally {
    contactSaving.value = false;
  }
}

function startCountdown(seconds: number) {
  if (countdownTimer != null) window.clearInterval(countdownTimer);
  captchaCountdown.value = seconds;
  countdownTimer = window.setInterval(() => {
    captchaCountdown.value -= 1;
    if (captchaCountdown.value <= 0 && countdownTimer != null) {
      window.clearInterval(countdownTimer);
      countdownTimer = undefined;
    }
  }, 1000);
}

async function loadAuthorizations() {
  if (!tenantId) {
    return;
  }
  authorizationLoading.value = true;
  try {
    const [available, currentBindings] = await Promise.all([
      listAvailableProviders(tenantId, appCode),
      listCurrentExternalIdentities(),
    ]);
    availableProviders.value = available;
    bindings.value = currentBindings;
  } catch (error) {
    console.error('加载第三方授权失败:', error);
  } finally {
    authorizationLoading.value = false;
  }
}

async function bindProvider(provider: ExternalAuthProvider) {
  if (!tenantId || bindingProvider.value) return;
  bindingProvider.value = provider;
  try {
    const authorization = await startProviderAuthorization({
      tenantId,
      appCode,
      provider,
      intent: 'BIND_CURRENT',
      redirectUri: providerCallbackUri(),
    });
    window.sessionStorage.setItem(PROVIDER_RETURN_KEY, '/profile?tab=authorization');
    window.location.assign(authorization.authorizationUrl);
  } catch (error) {
    console.error('发起第三方绑定失败:', error);
    bindingProvider.value = undefined;
  }
}

function openUnbindDialog(binding: ExternalIdentityBinding) {
  selectedBinding.value = binding;
  unbindPassword.value = '';
  unbindDialogVisible.value = true;
}

async function confirmUnbind() {
  if (!selectedBinding.value || !unbindPassword.value) {
    ElMessage.warning('请输入当前密码');
    return;
  }
  unbinding.value = true;
  try {
    await unbindCurrentExternalIdentity({
      bindingId: selectedBinding.value.id,
      currentPassword: unbindPassword.value,
    });
    unbindDialogVisible.value = false;
    ElMessage.success('第三方账号已解绑');
    await loadAuthorizations();
  } catch (error) {
    console.error('解绑第三方账号失败:', error);
  } finally {
    unbinding.value = false;
  }
}
</script>

<style scoped lang="scss">
.profile-container {
  max-width: 1240px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 24px);
}

.profile-shell {
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0;
  }
}

.settings-layout {
  display: grid;
  grid-template-columns: 244px minmax(0, 1fr);
  min-height: 680px;
}

.settings-sidebar {
  padding: 28px 20px;
  border-right: 1px solid var(--el-border-color-lighter);
  background: color-mix(in srgb, var(--el-fill-color-light) 62%, var(--el-bg-color));
}

.settings-sidebar__heading {
  padding: 0 12px 20px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    letter-spacing: 0.08em;
  }

  h2 {
    margin: 5px 0 0;
    color: var(--el-text-color-primary);
    font-size: 20px;
    line-height: 1.35;
  }
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);

  > div {
    display: grid;
    min-width: 0;
    gap: 3px;
  }

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 14px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.settings-menu {
  border-right: 0;
  background: transparent;

  :deep(.el-menu-item) {
    height: 46px;
    margin: 4px 0;
    border-radius: var(--el-border-radius-base);
    color: var(--el-text-color-regular);
  }

  :deep(.el-menu-item:hover) {
    background: var(--el-fill-color);
  }

  :deep(.el-menu-item.is-active) {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 600;
  }
}

.settings-content {
  min-width: 0;
  padding: clamp(24px, 4vw, 48px);
  background: var(--el-bg-color);
}

.content-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h1 {
    margin: 5px 0 8px;
    color: var(--el-text-color-primary);
    font-size: clamp(24px, 3vw, 30px);
    line-height: 1.25;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    line-height: 1.7;
  }
}

.content-eyebrow {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.profile-form {
  max-width: 820px;
}

.content-section {
  padding: 32px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.content-section--first {
  padding-top: 32px;
}

.section-heading,
.authorization-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;

  h2 {
    margin: 0 0 6px;
    color: var(--el-text-color-primary);
    font-size: 17px;
    line-height: 1.4;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.65;
  }
}

.verification-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 24px;
  width: 100%;
}

.avatar-preview {
  display: grid;
  flex: 0 0 auto;
  width: 96px;
  height: 96px;
  place-items: center;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  background: var(--el-fill-color-lighter);
}

.avatar-actions {
  display: grid;
  gap: 10px;

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.6;
  }
}

.avatar-action-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-hint {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-actions {
  padding-top: 24px;
}

.security-list {
  border-top: 1px solid var(--el-border-color-lighter);
}

.security-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 88px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.security-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 18px;
}

.security-copy {
  display: grid;
  gap: 5px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 14px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.dialog-description {
  margin: 0 0 20px;
  color: var(--el-text-color-secondary);
}

.profile-extension {
  padding-top: 24px;
}

.content-fade-enter-active,
.content-fade-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.content-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.content-fade-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

@media (width <= 768px) {
  .profile-container {
    padding: 12px;
  }

  .settings-layout {
    display: block;
    min-height: 0;
  }

  .settings-sidebar {
    padding: 20px;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .settings-sidebar__heading {
    padding: 0 0 16px;
  }

  .sidebar-user {
    margin-bottom: 14px;
    padding: 12px 0;
  }

  .settings-menu {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;

    :deep(.el-menu-item) {
      justify-content: center;
      min-width: 0;
      margin: 0;
      padding: 0 10px;
    }
  }

  .settings-content {
    padding: 24px 20px 32px;
  }
}

@media (width <= 520px) {
  .settings-sidebar {
    padding: 18px 16px;
  }

  .settings-menu {
    :deep(.el-menu-item) {
      gap: 3px;
      padding: 0 6px;
      font-size: 13px;
    }

    :deep(.el-menu-item .el-icon) {
      margin-right: 2px;
    }
  }

  .settings-content {
    padding: 22px 16px 28px;
  }

  .content-header {
    padding-bottom: 22px;
  }

  .content-section {
    padding: 26px 0;
  }

  .section-heading,
  .authorization-header {
    display: grid;
    gap: 14px;
  }

  .verification-summary {
    white-space: normal;
  }

  .avatar-editor {
    align-items: flex-start;
    gap: 16px;
  }

  .security-item {
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 12px;
    padding: 16px 0;
  }

  .security-item > .el-button {
    grid-column: 2;
    justify-self: start;
  }

  .captcha-row {
    display: grid;
  }
}

@media (prefers-reduced-motion: reduce) {
  .content-fade-enter-active,
  .content-fade-leave-active {
    transition: none;
  }
}
</style>
