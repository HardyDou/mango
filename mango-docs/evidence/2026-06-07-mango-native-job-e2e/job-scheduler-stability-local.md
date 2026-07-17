# Mango Job 本地调度稳定性证据

- jobCode: mango_job_stability_chromium_1784273509600
- jobName: 稳定性 每分钟任务 chromium
- jobId: 2078019829623517186
- schedule: 0 */1 * * * ?
- observeStartedAt: 2026-07-17T07:31:49.600Z
- observeEndedAt: 2026-07-17T07:34:51.825Z
- observeMinutes: 3
- expectedMinimumCompletedInstances: 2
- actualCompletedInstances: 3
- totalInstances: 3
- duplicateScheduledFireTimes: 0
- nonSuccessInstances: 0
- sampleInstanceId: 2078020396655669250
- sampleWorkerAddress: embedded://127.0.0.1:18007
- sampleLogContainsSystemOut: true
- sampleLogContainsLogger: true

## 观察快照

| time | completed | total |
|---|---:|---:|
| 2026-07-17T07:31:50.181Z | 0 | 0 |
| 2026-07-17T07:32:05.335Z | 1 | 1 |
| 2026-07-17T07:32:20.459Z | 1 | 1 |
| 2026-07-17T07:32:35.558Z | 1 | 1 |
| 2026-07-17T07:32:50.662Z | 1 | 1 |
| 2026-07-17T07:33:05.763Z | 2 | 2 |
| 2026-07-17T07:33:20.890Z | 2 | 2 |
| 2026-07-17T07:33:35.994Z | 2 | 2 |
| 2026-07-17T07:33:51.101Z | 2 | 2 |
| 2026-07-17T07:34:06.213Z | 3 | 3 |
| 2026-07-17T07:34:21.320Z | 3 | 3 |
| 2026-07-17T07:34:36.460Z | 3 | 3 |

## 完成实例窗口

| instanceId | scheduledFireTime | triggerTime | status | workerAddress |
|---|---|---|---|---|
| 2078019890759692289 | 2026-07-17 15:32:00 | 2026-07-17 15:32:04 | SUCCESS | embedded://127.0.0.1:18007 |
| 2078020143755915266 | 2026-07-17 15:33:00 | 2026-07-17 15:33:05 | SUCCESS | embedded://127.0.0.1:18007 |
| 2078020396655669250 | 2026-07-17 15:34:00 | 2026-07-17 15:34:05 | SUCCESS | embedded://127.0.0.1:18007 |
