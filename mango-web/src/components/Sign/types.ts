/**
 * Sign Component Types
 */

export interface SignProps {
  /** v-model binding - base64 signature image */
  modelValue?: string;
  /** Canvas width in px */
  width?: number;
  /** Canvas height in px */
  height?: number;
  /** Stroke color */
  strokeColor?: string;
  /** Stroke line width */
  lineWidth?: number;
  /** Disabled state */
  disabled?: boolean;
  /** Placeholder text when empty */
  placeholder?: string;
}

export interface SignEmits {
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}

export interface SignExpose {
  /** Clear the signature */
  clear(): void;
  /** Get current signature as base64 */
  getSignature(): string;
  /** Check if canvas is empty */
  isEmpty(): boolean;
}

export type SignInstance = InstanceType<typeof import('./index.vue').default>;
