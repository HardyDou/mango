import type { RouteLocationRaw } from 'vue-router';

export interface MangoPageBackBarProps {
  title: string;
  backLabel?: string;
  backTo?: RouteLocationRaw;
  navigateOnBack?: boolean;
  showRefresh?: boolean;
  refreshLoading?: boolean;
}

export interface MangoPageBackBarEmits {
  (event: 'back'): void;
  (event: 'refresh'): void;
}
