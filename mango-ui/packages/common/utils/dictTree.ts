import type { DictTreeOption } from '../api/dict';

export interface DictTreeIndexEntry {
  node: DictTreeOption;
  path: DictTreeOption[];
}

/**
 * 将树形字典展开为 value → 节点与根到当前节点路径的索引，避免多次递归查找。
 */
export function indexDictTree(
  nodes: DictTreeOption[],
  parents: DictTreeOption[] = [],
  index: Map<string, DictTreeIndexEntry> = new Map()
): Map<string, DictTreeIndexEntry> {
  for (const node of nodes) {
    const path = [...parents, node];
    index.set(String(node.value), { node, path });
    if (node.children?.length) {
      indexDictTree(node.children, path, index);
    }
  }
  return index;
}
