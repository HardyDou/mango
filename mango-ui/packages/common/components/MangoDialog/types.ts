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
  /** Whether to render a modal mask. Draggable dialogs default to no mask when omitted. */
  modal?: boolean;
  /** Whether clicking the modal mask closes the dialog */
  closeOnClickModal?: boolean;
  /** Whether opening the dialog locks document scrolling */
  lockScroll?: boolean;
  /** Minimum z-index used by this dialog before dynamic stacking */
  zIndex?: number;
  /** Whether the title area can drag the whole dialog */
  draggable?: boolean;
  /** Whether the four corners can resize the dialog */
  resizable?: boolean;
  /** Minimum interactive width in pixels */
  minWidth?: number;
  /** Minimum interactive height in pixels */
  minHeight?: number;
}

export interface MangoDialogEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'open'): void;
  (e: 'opened'): void;
  (e: 'close'): void;
  (e: 'closed'): void;
}
