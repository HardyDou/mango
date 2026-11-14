---
'@mango/file': minor
---

文件管理新增“按文件 ID 编辑信息”和“替换文件内容”：前端新增编辑弹窗与 `updateRecord`、`replaceContent` 两个 API，后端新增 `UpdateFileRecordCommand`、`ReplaceFileContentCommand`，上传会话支持 `replaceFileId` 复用已有文件记录，Flyway `V7` 为 `file_upload_session` 增加替换目标列。替换内容在大文件场景走分片会话，在普通文件场景走一次性上传，二者都保留文件 ID，文件名默认沿用原记录。
