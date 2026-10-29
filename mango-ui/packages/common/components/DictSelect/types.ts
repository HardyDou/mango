/** 字典形态：FLAT 平铺，TREE 树形。 */
export type DictShape = 'FLAT' | 'TREE';

/** FLAT 字典的选择控件样式；TREE 始终使用树形选择器。 */
export type DictSelectVariant = 'select' | 'radio-button' | 'checkbox-button';

/** 字典选择值：单选为标量，多选为数组。 */
export type DictSelectValue = string | number | Array<string | number> | undefined;

/** TREE 回显格式：label 只显示节点名，path 显示完整路径。 */
export type DictSelectDisplayFormat = 'label' | 'path';
