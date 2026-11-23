---
'@mango/information': minor
---

选择型字段支持绑定平台字典作为选项数据源（`optionSource: {type: "DICT", dictType}`），设计器新增「静态选项 / 平台字典」切换，托管页面、详情预览与采集记录列按字典启用项渲染选项；提交时只对声明了 `optionSource: DICT` 的字段按字典启用项校验值域；静态 `options` 仅用于渲染建议项，未声明 `optionSource` 的存量表单行为完全不变。
