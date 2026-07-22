# PLAN-RICH-TEXT-MANAGED-ASSETS 审批记录

- 审批人：Mango 实施负责人（当前会话用户）
- 审批日期：2026-07-22
- 审批结论：批准按 L3/FULL 实施计划进入编码与验证阶段。
- 审批依据：用户先明确“这个不是审批组件，我们要做的是富文本的基础能力”，随后在收到完整范围复述后确认“对的，就是这样”。
- 批准范围：`MangoEditor` 托管图片 token 双态、复制粘贴上传、服务端安全远程图片导入、token 回显、通用 `toolbar-actions` slot、Editor demo 中复用 `MUpload` 的附件基础示例，以及 TC-001～TC-007。
- 排除范围：不修改审批、workflow 或其它业务组件；不新增数据库表、字段、migration 或关联表；不持久化 Base64、Blob URL、预览 URL、下载 URL或第三方图片 URL。
- 安全门禁：无法保证 DNS 校验结果与实际连接目标一致时不得启用远程导入；任一 SSRF、租户、凭据泄漏或禁止 URL 持久化用例失败时停止交付。
- Git 边界：本次批准仅允许编码、测试和文档同步，不包含 commit、push、PR 或发布授权。
