# Mango Job 本地调度稳定性证据

- jobCode: mango_job_stability_chromium_1780843542233
- jobName: 稳定性 每分钟任务 chromium
- jobId: 2063633503600001025
- schedule: 0 */1 * * * ?
- observeStartedAt: 2026-06-07T14:45:42.233Z
- observeEndedAt: 2026-06-07T14:48:43.814Z
- observeMinutes: 3
- expectedMinimumCompletedInstances: 2
- actualCompletedInstances: 3
- totalInstances: 3
- duplicateScheduledFireTimes: 0
- nonSuccessInstances: 0
- sampleInstanceId: 2063634086956384258
- sampleWorkerAddress: in-memory://MacBookPro.local/embedded-29094@MacBookPro.local
- sampleLogContainsSystemOut: true
- sampleLogContainsLogger: true

## 观察快照

| time | completed | total |
|---|---:|---:|
| 2026-06-07T14:45:42.647Z | 0 | 0 |
| 2026-06-07T14:45:57.725Z | 0 | 0 |
| 2026-06-07T14:46:12.807Z | 1 | 1 |
| 2026-06-07T14:46:27.889Z | 1 | 1 |
| 2026-06-07T14:46:42.970Z | 1 | 1 |
| 2026-06-07T14:46:58.052Z | 1 | 1 |
| 2026-06-07T14:47:13.130Z | 2 | 2 |
| 2026-06-07T14:47:28.229Z | 2 | 2 |
| 2026-06-07T14:47:43.312Z | 2 | 2 |
| 2026-06-07T14:47:58.397Z | 2 | 2 |
| 2026-06-07T14:48:13.481Z | 3 | 3 |
| 2026-06-07T14:48:28.567Z | 3 | 3 |

## 完成实例窗口

| instanceId | scheduledFireTime | triggerTime | status | workerAddress |
|---|---|---|---|---|
| 2063633581475643394 | 2026-06-07 22:46:00 | 2026-06-07 22:46:01 | SUCCESS | in-memory://MacBookPro.local/embedded-29094@MacBookPro.local |
| 2063633834220208130 | 2026-06-07 22:47:00 | 2026-06-07 22:47:01 | SUCCESS | in-memory://MacBookPro.local/embedded-29094@MacBookPro.local |
| 2063634086956384258 | 2026-06-07 22:48:00 | 2026-06-07 22:48:01 | SUCCESS | in-memory://MacBookPro.local/embedded-29094@MacBookPro.local |
