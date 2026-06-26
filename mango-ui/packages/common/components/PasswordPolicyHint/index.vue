<template>
  <div class="password-policy-hint">
    <div class="password-strength">
      <span class="strength-label">密码强度</span>
      <span :class="['strength-value', `is-${strength}`]">{{ strengthLabel }}</span>
      <span class="strength-bars" aria-hidden="true">
        <span
          v-for="index in 3"
          :key="index"
          :class="{ active: index <= strengthLevel }"
        />
      </span>
    </div>
    <div class="password-rules">
      <span
        v-for="rule in rules"
        :key="rule.key"
        :class="['password-rule', { passed: rule.passed }]"
      >
        {{ rule.passed ? '✓' : '•' }} {{ rule.label }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  defaultPasswordPolicy,
  evaluatePasswordRules,
  getPasswordStrength,
  type PasswordPolicy,
} from '../../utils/passwordPolicy';

const props = withDefaults(defineProps<{
  password?: string;
  policy?: PasswordPolicy;
}>(), {
  password: '',
});

const policy = computed(() => props.policy || defaultPasswordPolicy);
const rules = computed(() => evaluatePasswordRules(props.password || '', policy.value));
const strength = computed(() => getPasswordStrength(props.password || '', policy.value));
const strengthLabel = computed(() => {
  if (strength.value === 'strong') return '强';
  if (strength.value === 'medium') return '中';
  if (strength.value === 'weak') return '弱';
  return '未输入';
});
const strengthLevel = computed(() => {
  if (strength.value === 'strong') return 3;
  if (strength.value === 'medium') return 2;
  if (strength.value === 'weak') return 1;
  return 0;
});
</script>

<style scoped>
.password-policy-hint {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-label {
  color: #909399;
}

.strength-value {
  min-width: 36px;
  font-weight: 600;
}

.strength-value.is-weak {
  color: #f56c6c;
}

.strength-value.is-medium {
  color: #e6a23c;
}

.strength-value.is-strong {
  color: #67c23a;
}

.strength-bars {
  display: inline-grid;
  grid-template-columns: repeat(3, 28px);
  gap: 4px;
}

.strength-bars span {
  height: 6px;
  border-radius: 3px;
  background: #e4e7ed;
}

.strength-bars span.active {
  background: #409eff;
}

.password-rules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}

.password-rule {
  color: #909399;
  white-space: nowrap;
}

.password-rule.passed {
  color: #67c23a;
}
</style>
