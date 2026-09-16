import { computed, onBeforeUnmount, onMounted, ref, type CSSProperties, type Ref } from 'vue';
import { useZIndex } from 'element-plus';

export type DialogResizeCorner = 'north-west' | 'north-east' | 'south-west' | 'south-east';

interface DialogRect {
  left: number;
  top: number;
  width: number;
  height: number;
}

interface DialogInteraction {
  kind: 'drag' | DialogResizeCorner;
  pointerId: number;
  pointerTarget: HTMLElement;
  startX: number;
  startY: number;
  startRect: DialogRect;
}

interface UseDialogWindowOptions {
  draggable: Readonly<Ref<boolean>>;
  resizable: Readonly<Ref<boolean>>;
  minWidth: Readonly<Ref<number>>;
  minHeight: Readonly<Ref<number>>;
  zIndex: Readonly<Ref<number | undefined>>;
}

const VIEWPORT_GAP = 24;
let highestDialogZIndex = 0;
const INTERACTIVE_SELECTOR = [
  'a',
  'button',
  'input',
  'select',
  'textarea',
  '[contenteditable="true"]',
  '[data-mango-dialog-no-drag]',
  '[role="button"]',
].join(',');

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function readRect(element: HTMLElement): DialogRect {
  const rect = element.getBoundingClientRect();
  return {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
  };
}

function findDialogElement(target: EventTarget | null) {
  return target instanceof Element ? target.closest<HTMLElement>('.mango-dialog.el-dialog') : null;
}

function isPrimaryPointer(event: PointerEvent) {
  return event.isPrimary && event.button === 0;
}

function readPixelValue(value: string) {
  const parsed = Number.parseFloat(value);
  return Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
}

function isPointerInsideElementPadding(event: PointerEvent, element: HTMLElement) {
  if (event.target !== element) return false;

  const rect = element.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return false;

  const styles = window.getComputedStyle(element);
  const scaleX = element.offsetWidth > 0 ? rect.width / element.offsetWidth : 1;
  const scaleY = element.offsetHeight > 0 ? rect.height / element.offsetHeight : 1;
  const borderLeft = readPixelValue(styles.borderLeftWidth) * scaleX;
  const borderRight = readPixelValue(styles.borderRightWidth) * scaleX;
  const borderTop = readPixelValue(styles.borderTopWidth) * scaleY;
  const borderBottom = readPixelValue(styles.borderBottomWidth) * scaleY;
  const paddingLeft = readPixelValue(styles.paddingLeft) * scaleX;
  const paddingRight = readPixelValue(styles.paddingRight) * scaleX;
  const paddingTop = readPixelValue(styles.paddingTop) * scaleY;
  const paddingBottom = readPixelValue(styles.paddingBottom) * scaleY;

  // clientWidth/clientHeight exclude scrollbars. Fall back to the computed border
  // box in non-layout test environments where client metrics are unavailable.
  const paddingBoxLeft = rect.left + (element.clientWidth > 0 ? element.clientLeft * scaleX : borderLeft);
  const paddingBoxTop = rect.top + (element.clientHeight > 0 ? element.clientTop * scaleY : borderTop);
  const paddingBoxRight =
    element.clientWidth > 0 ? paddingBoxLeft + element.clientWidth * scaleX : rect.right - borderRight;
  const paddingBoxBottom =
    element.clientHeight > 0 ? paddingBoxTop + element.clientHeight * scaleY : rect.bottom - borderBottom;
  const insidePaddingBox =
    event.clientX >= paddingBoxLeft &&
    event.clientX <= paddingBoxRight &&
    event.clientY >= paddingBoxTop &&
    event.clientY <= paddingBoxBottom;
  if (!insidePaddingBox) return false;

  const insideContentBox =
    event.clientX >= paddingBoxLeft + paddingLeft &&
    event.clientX <= paddingBoxRight - paddingRight &&
    event.clientY >= paddingBoxTop + paddingTop &&
    event.clientY <= paddingBoxBottom - paddingBottom;

  return !insideContentBox;
}

export function useDialogWindow(options: UseDialogWindowOptions) {
  const { nextZIndex } = useZIndex();
  const rect = ref<DialogRect | null>(null);
  const zIndex = ref<number>();
  const isInteracting = ref(false);
  const isBodyPaddingHovered = ref(false);
  let interaction: DialogInteraction | null = null;
  let previousCursor = '';
  let previousUserSelect = '';

  const dialogStyle = computed<CSSProperties>(() => {
    if (!rect.value) return {};

    return {
      position: 'fixed',
      left: `${rect.value.left}px`,
      top: `${rect.value.top}px`,
      width: `${rect.value.width}px`,
      height: `${rect.value.height}px`,
      maxWidth: 'none',
      maxHeight: 'none',
      margin: '0',
      transform: 'none',
    };
  });

  function bringToFront() {
    highestDialogZIndex = Math.max(highestDialogZIndex + 1, nextZIndex(), options.zIndex.value ?? 0);
    zIndex.value = highestDialogZIndex;
  }

  function getViewportSize() {
    return {
      width: Math.max(1, document.documentElement.clientWidth - VIEWPORT_GAP),
      height: Math.max(1, document.documentElement.clientHeight - VIEWPORT_GAP),
    };
  }

  function getResizeBounds() {
    const viewport = getViewportSize();
    return {
      maxWidth: viewport.width,
      maxHeight: viewport.height,
      minWidth: Math.min(Math.max(1, options.minWidth.value), viewport.width),
      minHeight: Math.min(Math.max(1, options.minHeight.value), viewport.height),
    };
  }

  function applyDrag(event: PointerEvent, currentInteraction: DialogInteraction) {
    const deltaX = event.clientX - currentInteraction.startX;
    const deltaY = event.clientY - currentInteraction.startY;
    rect.value = {
      ...currentInteraction.startRect,
      left: currentInteraction.startRect.left + deltaX,
      top: currentInteraction.startRect.top + deltaY,
    };
  }

  function applyResize(event: PointerEvent, currentInteraction: DialogInteraction) {
    const { minWidth, minHeight, maxWidth, maxHeight } = getResizeBounds();
    const { startRect } = currentInteraction;
    const deltaX = event.clientX - currentInteraction.startX;
    const deltaY = event.clientY - currentInteraction.startY;
    const fromWest = currentInteraction.kind.endsWith('west');
    const fromNorth = currentInteraction.kind.startsWith('north');
    const widthDelta = fromWest ? -deltaX : deltaX;
    const heightDelta = fromNorth ? -deltaY : deltaY;
    const width = clamp(startRect.width + widthDelta, minWidth, maxWidth);
    const height = clamp(startRect.height + heightDelta, minHeight, maxHeight);

    rect.value = {
      left: fromWest ? startRect.left + startRect.width - width : startRect.left,
      top: fromNorth ? startRect.top + startRect.height - height : startRect.top,
      width,
      height,
    };
  }

  function handlePointerMove(event: PointerEvent) {
    if (!interaction || event.pointerId !== interaction.pointerId) return;
    event.preventDefault();

    if (interaction.kind === 'drag') {
      applyDrag(event, interaction);
    } else {
      applyResize(event, interaction);
    }
  }

  function restoreDocumentInteractionStyles() {
    document.documentElement.style.cursor = previousCursor;
    document.documentElement.style.userSelect = previousUserSelect;
  }

  function finishInteraction(event?: PointerEvent) {
    if (!interaction || (event && event.pointerId !== interaction.pointerId)) return;

    if (interaction.pointerTarget.hasPointerCapture?.(interaction.pointerId)) {
      interaction.pointerTarget.releasePointerCapture(interaction.pointerId);
    }
    document.removeEventListener('pointermove', handlePointerMove);
    document.removeEventListener('pointerup', finishInteraction);
    document.removeEventListener('pointercancel', finishInteraction);
    restoreDocumentInteractionStyles();
    interaction = null;
    isInteracting.value = false;
  }

  function startInteraction(event: PointerEvent, kind: DialogInteraction['kind']) {
    if (!isPrimaryPointer(event)) return;
    const pointerTarget = event.currentTarget as HTMLElement;
    const dialogElement = findDialogElement(pointerTarget);
    if (!dialogElement) return;

    event.preventDefault();
    event.stopPropagation();
    finishInteraction();
    bringToFront();

    interaction = {
      kind,
      pointerId: event.pointerId,
      pointerTarget,
      startX: event.clientX,
      startY: event.clientY,
      startRect: readRect(dialogElement),
    };
    isInteracting.value = true;
    previousCursor = document.documentElement.style.cursor;
    previousUserSelect = document.documentElement.style.userSelect;
    document.documentElement.style.cursor = kind === 'drag' ? 'move' : window.getComputedStyle(pointerTarget).cursor;
    document.documentElement.style.userSelect = 'none';
    pointerTarget.setPointerCapture?.(event.pointerId);
    document.addEventListener('pointermove', handlePointerMove, { passive: false });
    document.addEventListener('pointerup', finishInteraction);
    document.addEventListener('pointercancel', finishInteraction);
  }

  function startDrag(event: PointerEvent) {
    if (!options.draggable.value || !isPrimaryPointer(event)) return;
    const target = event.target;
    if (target instanceof Element && target.closest(INTERACTIVE_SELECTOR)) {
      bringToFront();
      return;
    }
    startInteraction(event, 'drag');
  }

  function startBodyPaddingDrag(event: PointerEvent) {
    if (!options.draggable.value || !isPrimaryPointer(event)) return false;
    const element = event.currentTarget;
    if (!(element instanceof HTMLElement) || !isPointerInsideElementPadding(event, element)) return false;

    startInteraction(event, 'drag');
    return true;
  }

  function updateBodyPaddingHover(event: PointerEvent) {
    const element = event.currentTarget;
    isBodyPaddingHovered.value =
      options.draggable.value && element instanceof HTMLElement && isPointerInsideElementPadding(event, element);
  }

  function clearBodyPaddingHover() {
    isBodyPaddingHovered.value = false;
  }

  function startResize(event: PointerEvent, corner: DialogResizeCorner) {
    if (!options.resizable.value) return;
    startInteraction(event, corner);
  }

  function handleViewportResize() {
    if (!rect.value) return;
    const viewport = getViewportSize();
    rect.value = {
      ...rect.value,
      width: Math.min(rect.value.width, viewport.width),
      height: Math.min(rect.value.height, viewport.height),
    };
  }

  function resetWindow() {
    finishInteraction();
    clearBodyPaddingHover();
    rect.value = null;
  }

  onMounted(() => {
    window.addEventListener('resize', handleViewportResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleViewportResize);
    finishInteraction();
  });

  return {
    bringToFront,
    clearBodyPaddingHover,
    dialogStyle,
    isBodyPaddingHovered,
    isInteracting,
    resetWindow,
    startBodyPaddingDrag,
    startDrag,
    startResize,
    updateBodyPaddingHover,
    zIndex,
  };
}
