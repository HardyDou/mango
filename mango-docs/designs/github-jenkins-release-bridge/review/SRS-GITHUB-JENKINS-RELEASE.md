# SRS 审批证据

- 审批人：HardyDou
- 决定：APPROVED
- 日期：2026-07-14
- 依据：用户授权自行配置 GitHub 与 Jenkins，并明确先配置、后续把 Jenkins 主机升级到 16 vCPU / 32GB。
- 边界：当前只部署 Release Runner；公共 PR 继续使用 GitHub 托管 Runner；首次联通只执行 dry-run。
