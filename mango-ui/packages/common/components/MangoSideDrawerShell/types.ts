export interface MangoSideDrawerShellProps {
  modelValue?: boolean;
  title?: string;
  showTrigger?: boolean;
  drawerSize?: string | number;
  dataSurface?: string;
  dataAction?: string;
}

export interface MangoSideDrawerShellEmits {
  (event: 'update:modelValue', value: boolean): void;
  (event: 'open'): void;
  (event: 'close'): void;
}

export interface MangoSideDrawerShellExpose {
  open: () => void;
  close: () => void;
  toggle: () => void;
}
