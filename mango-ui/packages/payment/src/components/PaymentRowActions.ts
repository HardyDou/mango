import type { Component } from 'vue';
import type { ButtonProps } from 'element-plus';

export interface PaymentRowAction {
  key: string;
  label: string;
  type?: ButtonProps['type'];
  icon?: Component;
  visible?: boolean;
  disabled?: boolean;
  loading?: boolean;
  tooltip?: string;
  tooltipDisabled?: boolean;
  onClick: () => void | Promise<void>;
}
