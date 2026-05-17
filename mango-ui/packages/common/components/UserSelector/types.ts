export interface UserSelectorOption {
  value: string;
  label: string;
  username?: string;
  avatar?: string;
  meta?: string;
}

export interface UserSelectorProps {
  modelValue?: string | string[];
  mode?: 'select' | 'dialog';
  multiple?: boolean;
  placeholder?: string;
  title?: string;
  disabled?: boolean;
  width?: string | number;
  max?: number;
}

export interface UserSelectorEmits {
  (e: 'update:modelValue', value: string | string[] | undefined): void;
  (e: 'change', value: string | string[] | undefined): void;
}

export interface UserSelectorExpose {
  open: () => void;
  close: () => void;
  clear: () => void;
}
