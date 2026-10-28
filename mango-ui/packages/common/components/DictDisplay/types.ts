/** 字典形态：FLAT 平铺，TREE 树形。 */
export type DictDisplayShape = 'FLAT' | 'TREE';

/** 展示样式：text 纯文本，tag 标签。 */
export type DictDisplayMode = 'text' | 'tag';

/** TREE 展示格式：label 只显示节点名，path 显示完整路径。 */
export type DictDisplayFormat = 'label' | 'path';

/** 需要回显的值：单值或多值。 */
export type DictDisplayValue = string | number | Array<string | number> | null | undefined;

/** 单条解析结果。 */
export interface DictDisplayItem {
  /** 原始值统一转成字符串 */
  value: string;
  /** 节点 label，未命中字典时回退为原始值 */
  label: string;
  /** 根到当前节点的路径，FLAT 为空数组 */
  path: string[];
  /** 最终展示文本，受 displayFormat 影响 */
  text: string;
  /** 是否命中字典 */
  known: boolean;
}
