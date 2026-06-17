export type MangoDialogFooterAlign = 'left' | 'center' | 'right';

export interface MangoDialogProps {
  /** v-model binding for dialog visibility */
  modelValue: boolean;
  /** Header title text */
  title?: string;
  /** Dialog width, same semantics as Element Plus Dialog */
  width?: string | number;
  /** Whether to show the full title header */
  showHeader?: boolean;
  /** Whether to show the close icon */
  showClose?: boolean;
  /** Footer slot alignment */
  footerAlign?: MangoDialogFooterAlign;
  /** Destroy content when dialog is closed */
  destroyOnClose?: boolean;
}

export interface MangoDialogEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'open'): void;
  (e: 'opened'): void;
  (e: 'close'): void;
  (e: 'closed'): void;
}

