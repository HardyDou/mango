import type { Component } from 'vue';
import type {
  MangoDataTableProps,
  MangoDescriptionGroupExtraSlotProps,
  MangoDescriptionItemSlotProps,
  MangoDescriptionListProps,
  MangoDetailSummaryProps,
  MangoPageBackBarProps,
  MangoTableCellContext,
} from '@mango/common';
import type {
  MangoFileListCellSlotProps,
  MangoFileListFileActionsSlotProps,
  MangoFileListProps,
  MangoFileListRow,
} from '@mango/file';

export interface MangoDescriptionListPanelContent {
  componentType: 'description-list';
  data: MangoDescriptionListProps;
}

export interface MangoFileListPanelContent {
  componentType: 'file-list';
  data: MangoFileListProps;
}

export interface MangoRichTextPreviewPanelContent {
  componentType: 'rich-text-preview';
  data: { content?: string };
}

export interface MangoListTablePanelContent {
  componentType: 'list-table';
  data: Omit<MangoDataTableProps<Record<string, unknown>>, 'card' | 'mode' | 'showModeSwitch' | 'headerCellStyle'>;
}

export interface MangoCustomPanelContent {
  componentType: 'custom';
}

export type MangoCollapsePanelContent =
  | MangoDescriptionListPanelContent
  | MangoFileListPanelContent
  | MangoRichTextPreviewPanelContent
  | MangoListTablePanelContent
  | MangoCustomPanelContent;

export interface MangoCollapsePanelAction {
  name: string;
  label: string;
  icon?: Component;
  disabled?: boolean;
  loading?: boolean;
  dataAction?: string;
  dataStage?: string;
}

export interface MangoCollapsePanel {
  name: string;
  title: string;
  disabled?: boolean;
  emptyDescription?: string;
  dataSurface?: string;
  headerActions?: MangoCollapsePanelAction[];
  content?: MangoCollapsePanelContent;
}

export interface MangoCollapseTab {
  name: string;
  label: string;
  disabled?: boolean;
  lazy?: boolean;
  panels?: MangoCollapsePanel[];
  emptyDescription?: string;
}

export interface MangoCollapseResponsive {
  gutter?: number;
  span?: number;
  xs?: number;
  sm?: number;
  md?: number;
  lg?: number;
  xl?: number;
}

export type MangoCollapseActionsAlign = 'left' | 'center' | 'right';

export interface MangoCollapseDetailPageProps {
  title: string;
  panels?: MangoCollapsePanel[];
  tabs?: MangoCollapseTab[];
  responsive?: MangoCollapseResponsive;
  activeTab?: string;
  defaultActiveTab?: string;
  autoExpandPanels?: boolean;
  defaultActivePanelNames?: string[];
  backLabel?: string;
  backTo?: MangoPageBackBarProps['backTo'];
  navigateOnBack?: boolean;
  showBackBar?: boolean;
  showRefresh?: boolean;
  refreshLoading?: boolean;
  showContentShadow?: boolean;
  summary?: MangoDetailSummaryProps;
  showBackTop?: boolean;
  showActions?: boolean;
  actionsAlign?: MangoCollapseActionsAlign;
  backTopThreshold?: number;
  showWorkflow?: boolean;
  showWorkflowTrigger?: boolean;
  workflowTitle?: string;
  workflowDrawerSize?: string | number;
  dataPage?: string;
  loading?: boolean;
  errorText?: string;
  emptyDescription?: string;
}

export interface MangoCollapsePanelSlotProps {
  panel: MangoCollapsePanel;
  index: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapsePanelActionEvent {
  action: MangoCollapsePanelAction;
  panel: MangoCollapsePanel;
  panelIndex: number;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseTabSlotProps {
  tab: MangoCollapseTab;
  index: number;
  active: boolean;
}

export interface MangoCollapseDescriptionItemSlotProps extends MangoDescriptionItemSlotProps {
  panel: MangoCollapsePanel;
  panelIndex: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseDescriptionGroupExtraSlotProps extends MangoDescriptionGroupExtraSlotProps {
  panel: MangoCollapsePanel;
  panelIndex: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseFileListCellSlotProps extends MangoFileListCellSlotProps {
  panel: MangoCollapsePanel;
  panelIndex: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseFileListFileActionsSlotProps extends MangoFileListFileActionsSlotProps {
  panel: MangoCollapsePanel;
  panelIndex: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseListTableCellSlotProps extends MangoTableCellContext<Record<string, unknown>> {
  panel: MangoCollapsePanel;
  panelIndex: number;
  expanded: boolean;
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseFileListSelectionChange {
  panel: MangoCollapsePanel;
  panelIndex: number;
  rows: MangoFileListRow[];
  tab?: MangoCollapseTab;
  tabIndex?: number;
}

export interface MangoCollapseDetailPageEmits {
  (event: 'back'): void;
  (event: 'refresh'): void;
  (event: 'retry'): void;
  (event: 'update:activeTab', name: string): void;
  (event: 'change', names: string[]): void;
  (event: 'tab-change', name: string): void;
  (event: 'file-list-selection-change', payload: MangoCollapseFileListSelectionChange): void;
  (event: 'panel-action', payload: MangoCollapsePanelActionEvent): void;
}

export interface MangoCollapseDetailPageExpose {
  openWorkflowDrawer: () => void;
  closeWorkflowDrawer: () => void;
  toggleWorkflowDrawer: () => void;
  scrollToPageTop: () => void;
}
