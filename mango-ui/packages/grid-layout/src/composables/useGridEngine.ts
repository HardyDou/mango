import type { GridLayoutItem, GridLayoutOptions, GridLayoutRect, GridWidgetDefinition } from '../types';

const DEFAULT_COLUMNS = 12;
const DEFAULT_CARD_WIDTH = 3;
const DEFAULT_CARD_HEIGHT = 10;
const MAX_SCAN_ROWS = 1000;

export function normalizeLayoutItems(items: GridLayoutItem[], options?: Partial<GridLayoutOptions>): GridLayoutItem[] {
  const columns = options?.columns || DEFAULT_COLUMNS;
  const normalized = items.map(item => ({
    ...item,
    layout: normalizeRect(item.layout, columns),
  }));
  if (options?.compact === false) {
    return normalized.sort((first, second) => first.layout.y - second.layout.y || first.layout.x - second.layout.x);
  }
  return resolveCollisions(normalized, columns, undefined, options?.verticalGapRows);
}

export function normalizeRect(rect: GridLayoutRect, columns = DEFAULT_COLUMNS): GridLayoutRect {
  const minW = clampNumber(Math.max(rect.minW ?? DEFAULT_CARD_WIDTH, DEFAULT_CARD_WIDTH), 1, columns);
  const minH = clampNumber(Math.max(rect.minH ?? DEFAULT_CARD_HEIGHT, DEFAULT_CARD_HEIGHT), 1, MAX_SCAN_ROWS);
  const maxW = clampNumber(rect.maxW ?? columns, minW, columns);
  const maxH = clampNumber(rect.maxH ?? MAX_SCAN_ROWS, minH, MAX_SCAN_ROWS);
  const w = clampNumber(rect.w || 1, minW, maxW);
  const h = clampNumber(rect.h || 1, minH, maxH);
  return {
    ...rect,
    minW,
    minH,
    maxW,
    maxH,
    w,
    h,
    x: clampNumber(rect.x || 0, 0, columns - w),
    y: Math.max(0, rect.y || 0),
  };
}

export function moveGridItem(
  items: GridLayoutItem[],
  itemId: string,
  nextRect: GridLayoutRect,
  columns = DEFAULT_COLUMNS,
  verticalGapRows = 0,
): GridLayoutItem[] {
  const moved = items.map(item => item.id === itemId
    ? { ...item, layout: normalizeRect({ ...item.layout, ...nextRect }, columns) }
    : cloneGridItem(item));
  return resolveCollisions(moved, columns, itemId, verticalGapRows);
}

export function resizeGridItem(
  items: GridLayoutItem[],
  itemId: string,
  nextRect: Partial<GridLayoutRect>,
  columns = DEFAULT_COLUMNS,
  verticalGapRows = 0,
): GridLayoutItem[] {
  const resized = items.map(item => item.id === itemId
    ? { ...item, layout: normalizeRect({ ...item.layout, ...nextRect }, columns) }
    : cloneGridItem(item));
  return resolveCollisions(resized, columns, itemId, verticalGapRows);
}

export function removeGridItem(
  items: GridLayoutItem[],
  itemId: string,
  columns = DEFAULT_COLUMNS,
  verticalGapRows = 0,
): GridLayoutItem[] {
  return resolveCollisions(items.filter(item => item.id !== itemId).map(cloneGridItem), columns, undefined, verticalGapRows);
}

export function createGridItemFromWidget(
  widget: GridWidgetDefinition,
  items: GridLayoutItem[],
  rect?: Partial<GridLayoutRect>,
  options?: Partial<GridLayoutOptions>,
): GridLayoutItem {
  const columns = options?.columns || DEFAULT_COLUMNS;
  const baseRect = normalizeRect({
    x: 0,
    y: 0,
    w: options?.defaultWidth || DEFAULT_CARD_WIDTH,
    h: options?.defaultHeight || DEFAULT_CARD_HEIGHT,
    ...widget.defaultLayout,
    ...rect,
  }, columns);
  const layout = findAvailableRect(items, baseRect, columns, options?.verticalGapRows);
  return {
    id: `${widget.type}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    widgetType: widget.type,
    title: widget.title,
    layout,
    props: widget.defaultProps ? { ...widget.defaultProps } : undefined,
    showTitle: widget.showTitle,
    padding: widget.padding,
  };
}

export function getGridRows(items: GridLayoutItem[], minRows = 4): number {
  return Math.max(minRows, ...items.map(item => item.layout.y + item.layout.h));
}

export function rectsOverlap(first: GridLayoutRect, second: GridLayoutRect): boolean {
  return first.x < second.x + second.w
    && first.x + first.w > second.x
    && first.y < second.y + second.h
    && first.y + first.h > second.y;
}

function resolveCollisions(items: GridLayoutItem[], columns: number, priorityId?: string, verticalGapRows = 0): GridLayoutItem[] {
  if (priorityId) {
    // 用户正在操作的卡片优先保持目标位置，其它卡片只在真实碰撞时才让位。
    return resolveByStableInsert(items, columns, priorityId, verticalGapRows);
  }
  const sorted = [...items].sort((first, second) => {
    return first.layout.y - second.layout.y || first.layout.x - second.layout.x;
  });
  const placed: GridLayoutItem[] = [];
  sorted.forEach((item) => {
    const candidate = cloneGridItem(item);
    candidate.layout = findAvailableRect(placed, candidate.layout, columns, verticalGapRows);
    placed.push(candidate);
  });
  return placed.sort((first, second) => first.layout.y - second.layout.y || first.layout.x - second.layout.x);
}

function resolveByStableInsert(
  items: GridLayoutItem[],
  columns: number,
  priorityId: string,
  verticalGapRows: number,
): GridLayoutItem[] {
  const priorityItem = items.find(item => item.id === priorityId);
  if (!priorityItem) {
    return resolveCollisions(items, columns, undefined, verticalGapRows);
  }
  const priority = cloneGridItem(priorityItem);
  priority.layout = normalizeRect(priority.layout, columns);
  const orderedRest = items
    .filter(item => item.id !== priorityId)
    .map(cloneGridItem)
    .sort(compareItemsByPosition);

  // 垂直 gap 是用虚拟行表达的，向上拖进上方卡片的 gap 区时不应挤开上方卡片。
  priority.layout = snapOutOfTopGap(priority.layout, orderedRest, columns, verticalGapRows);
  const placed: GridLayoutItem[] = [priority];
  orderedRest.forEach((item) => {
    const candidate = cloneGridItem(item);
    candidate.layout = normalizeRect(candidate.layout, columns);
    if (placed.some(placedItem => rectsOverlap(expandRect(candidate.layout, verticalGapRows), expandRect(placedItem.layout, verticalGapRows)))) {
      candidate.layout = findAvailableRect(placed, candidate.layout, columns, verticalGapRows);
    }
    placed.push(candidate);
  });
  return placed.sort(compareItemsByPosition);
}

function snapOutOfTopGap(
  rect: GridLayoutRect,
  items: GridLayoutItem[],
  columns: number,
  verticalGapRows: number,
): GridLayoutRect {
  if (verticalGapRows <= 0) {
    return rect;
  }
  const normalized = normalizeRect(rect, columns);
  // 只处理“落在卡片底部间距里但没有压住卡片内容”的场景。
  const blocker = items.find((item) => {
    const itemBottom = item.layout.y + item.layout.h;
    return normalized.x < item.layout.x + item.layout.w
      && normalized.x + normalized.w > item.layout.x
      && normalized.y >= itemBottom
      && normalized.y < itemBottom + verticalGapRows
      && !rectsOverlap(normalized, item.layout);
  });
  if (!blocker) {
    return normalized;
  }
  // 吸附到该卡片的间距之后，再找一个不重叠的位置。
  return findAvailableRect(items, {
    ...normalized,
    y: blocker.layout.y + blocker.layout.h + verticalGapRows,
  }, columns, verticalGapRows);
}

function compareItemsByPosition(first: GridLayoutItem, second: GridLayoutItem): number {
  return compareRectsByPosition(first.layout, second.layout);
}

function compareRectsByPosition(first: GridLayoutRect, second: GridLayoutRect): number {
  return first.y - second.y || first.x - second.x;
}

export function findAvailableRect(items: GridLayoutItem[], rect: GridLayoutRect, columns: number, verticalGapRows = 0): GridLayoutRect {
  const normalized = normalizeRect(rect, columns);
  for (let y = normalized.y; y < normalized.y + MAX_SCAN_ROWS; y += 1) {
    const startX = y === normalized.y ? normalized.x : 0;
    for (let x = startX; x <= columns - normalized.w; x += 1) {
      const candidate = { ...normalized, x, y };
      if (!items.some(item => rectsOverlap(expandRect(candidate, verticalGapRows), expandRect(item.layout, verticalGapRows)))) {
        return candidate;
      }
    }
  }
  return { ...normalized, x: 0, y: getGridRows(items) };
}

function expandRect(rect: GridLayoutRect, verticalGapRows: number): GridLayoutRect {
  if (verticalGapRows <= 0) {
    return rect;
  }
  return {
    ...rect,
    h: rect.h + verticalGapRows,
  };
}

function cloneGridItem(item: GridLayoutItem): GridLayoutItem {
  return {
    ...item,
    layout: { ...item.layout },
    props: item.props ? { ...item.props } : undefined,
  };
}

function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, Math.round(value)));
}
