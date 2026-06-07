# Mango Job 本地调度稳定性证据

- jobCode: mango_job_stability_chromium_1780824592232
- jobName: 稳定性 每分钟任务 chromium
- jobId: 2063554022499729410
- schedule: 0 */1 * * * ?
- observeStartedAt: 2026-06-07T09:29:52.232Z
- observeEndedAt: 2026-06-07T09:32:54.379Z
- observeMinutes: 3
- expectedMinimumCompletedInstances: 2
- actualCompletedInstances: 3
- totalInstances: 3
- duplicateScheduledFireTimes: 0
- nonSuccessInstances: 0
- sampleInstanceId: 2063554571408293890
- sampleWorkerAddress: in-memory://MacBookPro.local/38485@MacBookPro.local
- sampleLogContainsSystemOut: true
- sampleLogContainsLogger: true

## 观察快照

| time | completed | total |
|---|---:|---:|
| 2026-06-07T09:29:52.938Z | 0 | 0 |
| 2026-06-07T09:30:08.038Z | 1 | 1 |
| 2026-06-07T09:30:23.145Z | 1 | 1 |
| 2026-06-07T09:30:38.243Z | 1 | 1 |
| 2026-06-07T09:30:53.343Z | 1 | 1 |
| 2026-06-07T09:31:08.447Z | 2 | 2 |
| 2026-06-07T09:31:23.562Z | 2 | 2 |
| 2026-06-07T09:31:38.672Z | 2 | 2 |
| 2026-06-07T09:31:53.772Z | 2 | 2 |
| 2026-06-07T09:32:08.875Z | 3 | 3 |
| 2026-06-07T09:32:23.980Z | 3 | 3 |
| 2026-06-07T09:32:39.084Z | 3 | 3 |

## 完成实例窗口

| instanceId | scheduledFireTime | triggerTime | status | workerAddress |
|---|---|---|---|---|
| 2063554062601469953 | 2026-06-07 17:30:00 | 2026-06-07 17:30:02 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063554316944064513 | 2026-06-07 17:31:00 | 2026-06-07 17:31:03 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
| 2063554571408293890 | 2026-06-07 17:32:00 | 2026-06-07 17:32:03 | SUCCESS | in-memory://MacBookPro.local/38485@MacBookPro.local |
